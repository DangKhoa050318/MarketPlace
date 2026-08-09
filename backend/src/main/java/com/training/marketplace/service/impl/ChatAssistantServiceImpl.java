package com.training.marketplace.service.impl;

import com.training.marketplace.config.ChatAssistantProperties;
import com.training.marketplace.analytics.AnalyticsEventSource;
import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.dto.request.ChatMessageRequest;
import com.training.marketplace.dto.request.ChatPageContext;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.dto.response.ChatMessageResponse;
import com.training.marketplace.dto.response.ChatProductCardResponse;
import com.training.marketplace.dto.response.ChatVoucherResponse;
import com.training.marketplace.enums.ChatIntent;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ForbiddenException;
import com.training.marketplace.service.CampaignOfferService;
import com.training.marketplace.service.AnalyticsEventService;
import com.training.marketplace.service.ChatAiGateway;
import com.training.marketplace.service.ChatAssistantService;
import com.training.marketplace.service.ChatCampaignOffer;
import com.training.marketplace.service.ChatConversationSnapshot;
import com.training.marketplace.service.ChatConversationStore;
import com.training.marketplace.service.ChatHistoryMessage;
import com.training.marketplace.service.ChatIntentAnalysis;
import com.training.marketplace.service.ChatProductCandidate;
import com.training.marketplace.service.ChatProductDiscoveryService;
import com.training.marketplace.service.ChatSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatAssistantServiceImpl implements ChatAssistantService {

    private static final int MAX_SESSION_ID_LENGTH = 128;

    private final ChatAssistantProperties properties;
    private final ChatConversationStore conversationStore;
    private final ChatAiGateway aiGateway;
    private final RuleBasedChatIntentAnalyzer fallbackAnalyzer;
    private final CampaignOfferService campaignOfferService;
    private final ChatProductDiscoveryService productDiscoveryService;
    private final AnalyticsEventService analyticsEventService;

    @Override
    public ChatMessageResponse reply(
            Long userId,
            String sessionId,
            ChatMessageRequest request) {
        if (!properties.isEnabled()) {
            throw new BadRequestException("Chat assistant is currently disabled");
        }
        String normalizedSessionId = normalizeSessionId(sessionId);
        String ownerKey = ownerKey(userId, normalizedSessionId);
        UUID conversationId = request.conversationId() == null
                ? UUID.randomUUID() : request.conversationId();
        ChatConversationSnapshot conversation = loadConversation(
                conversationId, ownerKey, userId, normalizedSessionId);

        ChatIntentAnalysis fallback = fallbackAnalyzer.analyze(
                request.message(), conversation.messages(), request.pageContext());
        ChatIntentAnalysis analysis = merge(
                aiGateway.analyze(request.message(), conversation.messages(), request.pageContext())
                        .orElse(fallback),
                fallback);

        UUID messageId = UUID.randomUUID();
        UUID traceId = UUID.randomUUID();
        if (analysis.needsClarification()) {
            String question = analysis.clarificationQuestion() == null
                    ? "Bạn đang tìm dòng sản phẩm nào và ngân sách dự kiến là bao nhiêu?"
                    : analysis.clarificationQuestion();
            saveExchange(conversation, ownerKey, request.message(), question);
            trackChatEvent(userId, normalizedSessionId, analysis, 0, false);
            return new ChatMessageResponse(
                    conversationId, messageId, question, analysis.intents(), List.of(),
                    List.of("Sản phẩm bán chạy", "Sản phẩm đang có voucher"), traceId);
        }

        boolean wantsOffers = analysis.intents().contains(ChatIntent.CAMPAIGN_OFFERS);
        boolean wantsBestSeller = analysis.intents().contains(ChatIntent.BEST_SELLER);
        List<ChatCampaignOffer> offers = wantsOffers
                ? campaignOfferService.findEffectiveOffers(userId) : List.of();
        if (wantsOffers && offers.isEmpty()) {
            String answer = "Hiện chưa có campaign kèm voucher nào còn hiệu lực. "
                    + "Bạn có thể hỏi mình về sản phẩm bán chạy hoặc nêu nhu cầu để mình tư vấn.";
            saveExchange(conversation, ownerKey, request.message(), answer);
            trackChatEvent(userId, normalizedSessionId, analysis, 0, false);
            return new ChatMessageResponse(
                    conversationId, messageId, answer, analysis.intents(), List.of(),
                    List.of("Xem sản phẩm bán chạy", "Tư vấn theo ngân sách"), traceId);
        }

        Set<Long> productScopeIds = new LinkedHashSet<>();
        Set<Long> categoryScopeIds = new LinkedHashSet<>();
        boolean cartWideOffer = collectOfferScopes(
                offers, productScopeIds, categoryScopeIds);
        ChatPageContext pageContext = request.pageContext();
        ChatSearchCriteria criteria = new ChatSearchCriteria(
                combinedQuery(analysis),
                analysis.category(),
                pageContext == null ? null : pageContext.categoryId(),
                lowerPrice(analysis),
                upperPrice(analysis),
                analysis.brand(),
                productScopeIds,
                categoryScopeIds,
                wantsOffers,
                cartWideOffer,
                properties.getMaxProducts());

        List<ChatProductCandidate> candidates = productDiscoveryService.discover(
                criteria, wantsBestSeller, userId, normalizedSessionId);
        List<ChatProductCardResponse> productCards = candidates.stream()
                .map(candidate -> toCard(candidate, offers, wantsBestSeller))
                .toList();
        String answer = answer(productCards, wantsBestSeller, wantsOffers);
        saveExchange(conversation, ownerKey, request.message(), answer);
        trackChatEvent(userId, normalizedSessionId, analysis, productCards.size(),
                productCards.stream().anyMatch(product -> product.voucher() != null));
        return new ChatMessageResponse(
                conversationId,
                messageId,
                answer,
                analysis.intents(),
                productCards,
                quickReplies(analysis.intents(), productCards.isEmpty()),
                traceId);
    }

    private ChatConversationSnapshot loadConversation(
            UUID conversationId,
            String ownerKey,
            Long userId,
            String sessionId) {
        return conversationStore.find(conversationId)
                .map(existing -> {
                    if (ownerKey.equals(existing.ownerKey())) {
                        return existing;
                    }
                    if (userId != null && sessionId != null
                            && ("s:" + sessionId).equals(existing.ownerKey())) {
                        return new ChatConversationSnapshot(
                                existing.conversationId(), ownerKey, existing.messages());
                    }
                    if (!ownerKey.equals(existing.ownerKey())) {
                        throw new ForbiddenException("Conversation does not belong to the current visitor");
                    }
                    return existing;
                })
                .orElseGet(() -> new ChatConversationSnapshot(conversationId, ownerKey, List.of()));
    }

    private ChatIntentAnalysis merge(ChatIntentAnalysis primary, ChatIntentAnalysis fallback) {
        List<ChatIntent> intents = primary.intents() == null || primary.intents().isEmpty()
                ? fallback.intents() : primary.intents();
        return new ChatIntentAnalysis(
                intents,
                first(primary.query(), fallback.query()),
                first(primary.category(), fallback.category()),
                primary.minPrice() == null ? fallback.minPrice() : primary.minPrice(),
                primary.maxPrice() == null ? fallback.maxPrice() : primary.maxPrice(),
                first(primary.brand(), fallback.brand()),
                primary.attributes().isEmpty() ? fallback.attributes() : primary.attributes(),
                primary.needsClarification() && fallback.needsClarification(),
                first(primary.clarificationQuestion(), fallback.clarificationQuestion()));
    }

    private String combinedQuery(ChatIntentAnalysis analysis) {
        List<String> parts = new ArrayList<>();
        if (analysis.query() != null && !analysis.query().isBlank()) {
            parts.add(analysis.query());
        }
        analysis.attributes().values().stream()
                .filter(value -> value != null && !value.isBlank())
                .forEach(parts::add);
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private BigDecimal lowerPrice(ChatIntentAnalysis analysis) {
        if (analysis.minPrice() != null && analysis.maxPrice() != null
                && analysis.minPrice().compareTo(analysis.maxPrice()) > 0) {
            return analysis.maxPrice();
        }
        return analysis.minPrice();
    }

    private BigDecimal upperPrice(ChatIntentAnalysis analysis) {
        if (analysis.minPrice() != null && analysis.maxPrice() != null
                && analysis.minPrice().compareTo(analysis.maxPrice()) > 0) {
            return analysis.minPrice();
        }
        return analysis.maxPrice();
    }

    private boolean collectOfferScopes(
            List<ChatCampaignOffer> offers,
            Set<Long> productScopeIds,
            Set<Long> categoryScopeIds) {
        boolean cartWide = false;
        for (ChatCampaignOffer offer : offers) {
            if (offer.scopeType() == PromotionScopeType.CART) {
                cartWide = true;
            }
            productScopeIds.addAll(offer.scopes().getOrDefault(ScopeRefType.PRODUCT, Set.of()));
            categoryScopeIds.addAll(offer.scopes().getOrDefault(ScopeRefType.CATEGORY, Set.of()));
        }
        return cartWide;
    }

    private ChatProductCardResponse toCard(
            ChatProductCandidate candidate,
            List<ChatCampaignOffer> offers,
            boolean bestSeller) {
        ChatCampaignOffer offer = offers.stream()
                .filter(item -> item.appliesTo(candidate.productId(), candidate.categoryId()))
                .max(Comparator.comparingDouble(this::offerScore))
                .orElse(null);
        String reason = bestSeller
                ? "Đang nằm trong nhóm sản phẩm bán chạy và còn hàng."
                : "Phù hợp với nhu cầu đã nêu và hiện còn hàng.";
        if (offer != null) {
            reason += " Có voucher trong campaign " + offer.campaignName() + ".";
        }
        return new ChatProductCardResponse(
                candidate.productId(),
                candidate.slug(),
                candidate.name(),
                candidate.imageUrl(),
                candidate.categoryId(),
                candidate.categoryName(),
                candidate.brand(),
                candidate.minPrice(),
                candidate.maxPrice(),
                candidate.availableStock(),
                reason,
                offer == null ? null : toVoucher(offer));
    }

    private ChatVoucherResponse toVoucher(ChatCampaignOffer offer) {
        return new ChatVoucherResponse(
                offer.campaignId(),
                offer.campaignName(),
                offer.code(),
                offer.discountType().name(),
                offer.discountValue(),
                offer.maxDiscount(),
                offer.minOrderAmount(),
                offer.scopeType().name(),
                offer.expiresAt(),
                discountText(offer));
    }

    private String discountText(ChatCampaignOffer offer) {
        String discount = offer.discountType() == DiscountType.PERCENT
                ? "Giảm " + offer.discountValue().stripTrailingZeros().toPlainString() + "%"
                : "Giảm " + money(offer.discountValue());
        if (offer.maxDiscount() != null && offer.discountType() == DiscountType.PERCENT) {
            discount += ", tối đa " + money(offer.maxDiscount());
        }
        if (offer.minOrderAmount() != null && offer.minOrderAmount().signum() > 0) {
            discount += ", đơn tối thiểu " + money(offer.minOrderAmount());
        }
        return discount;
    }

    private double offerScore(ChatCampaignOffer offer) {
        if (offer.discountType() == DiscountType.PERCENT) {
            return offer.discountValue().doubleValue() * 100_000;
        }
        return offer.discountValue().doubleValue();
    }

    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN")).format(value);
    }

    private String answer(
            List<ChatProductCardResponse> products,
            boolean bestSeller,
            boolean offers) {
        if (products.isEmpty()) {
            return "Mình chưa tìm thấy sản phẩm còn hàng đáp ứng đầy đủ các điều kiện. "
                    + "Bạn thử nới ngân sách hoặc chọn một danh mục khác nhé.";
        }
        StringBuilder answer = new StringBuilder("Mình tìm thấy ")
                .append(products.size()).append(" sản phẩm phù hợp");
        if (bestSeller) {
            answer.append(" trong nhóm bán chạy");
        }
        if (offers) {
            long voucherCount = products.stream().filter(item -> item.voucher() != null).count();
            answer.append(", trong đó ").append(voucherCount)
                    .append(" sản phẩm có voucher campaign đang hiệu lực");
        }
        return answer.append(". Bạn có thể mở từng sản phẩm để xem SKU và giá chi tiết.").toString();
    }

    private List<String> quickReplies(List<ChatIntent> intents, boolean empty) {
        if (empty) {
            return List.of("Tăng ngân sách", "Bỏ điều kiện voucher", "Xem sản phẩm bán chạy");
        }
        if (intents.contains(ChatIntent.CAMPAIGN_OFFERS)) {
            return List.of("Ưu tiên giảm giá cao", "Chỉ xem sản phẩm bán chạy", "Xem chi tiết voucher");
        }
        return List.of("Ưu tiên giá thấp", "Sản phẩm có voucher", "Sản phẩm bán chạy");
    }

    private void saveExchange(
            ChatConversationSnapshot conversation,
            String ownerKey,
            String userMessage,
            String assistantMessage) {
        List<ChatHistoryMessage> messages = new ArrayList<>(conversation.messages());
        messages.add(new ChatHistoryMessage("user", userMessage));
        messages.add(new ChatHistoryMessage("assistant", assistantMessage));
        int fromIndex = Math.max(0, messages.size() - properties.getMaxHistoryMessages());
        conversationStore.save(new ChatConversationSnapshot(
                conversation.conversationId(), ownerKey, messages.subList(fromIndex, messages.size())));
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        String normalized = sessionId.trim();
        if (normalized.length() > MAX_SESSION_ID_LENGTH) {
            throw new BadRequestException("X-Session-Id must not exceed 128 characters");
        }
        return normalized;
    }

    private String ownerKey(Long userId, String sessionId) {
        if (userId != null) {
            return "u:" + userId;
        }
        if (sessionId == null) {
            throw new BadRequestException("X-Session-Id is required for anonymous chat");
        }
        return "s:" + sessionId;
    }

    private String first(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }

    private void trackChatEvent(
            Long userId,
            String sessionId,
            ChatIntentAnalysis analysis,
            int productCount,
            boolean hasVoucher) {
        try {
            analyticsEventService.track(userId, sessionId, new TrackAnalyticsEventRequest(
                    UUID.randomUUID(),
                    TrackAnalyticsEventRequest.CURRENT_SCHEMA_VERSION,
                    AnalyticsEventType.CHAT_MESSAGE,
                    Instant.now(),
                    null,
                    null,
                    AnalyticsEventSource.CHAT_ASSISTANT,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of(
                            "intents", analysis.intents().stream().map(Enum::name).toList(),
                            "productCount", productCount,
                            "hasVoucher", hasVoucher)));
        } catch (RuntimeException ignored) {
            // Analytics is best-effort and must never break the shopping conversation.
        }
    }
}
