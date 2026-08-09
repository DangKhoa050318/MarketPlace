package com.training.marketplace.service;

import com.training.marketplace.config.ChatAssistantProperties;
import com.training.marketplace.dto.request.ChatMessageRequest;
import com.training.marketplace.enums.ChatIntent;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ForbiddenException;
import com.training.marketplace.service.impl.ChatAssistantServiceImpl;
import com.training.marketplace.service.impl.RuleBasedChatIntentAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAssistantServiceTest {

    @Mock private ChatConversationStore conversationStore;
    @Mock private ChatAiGateway aiGateway;
    @Mock private CampaignOfferService campaignOfferService;
    @Mock private ChatProductDiscoveryService discoveryService;
    @Mock private AnalyticsEventService analyticsEventService;

    private ChatAssistantService service;

    @BeforeEach
    void setUp() {
        ChatAssistantProperties properties = new ChatAssistantProperties();
        properties.setMaxProducts(6);
        properties.setMaxHistoryMessages(12);
        service = new ChatAssistantServiceImpl(
                properties,
                conversationStore,
                aiGateway,
                new RuleBasedChatIntentAnalyzer(),
                campaignOfferService,
                discoveryService,
                analyticsEventService);
    }

    @Test
    void returnsInStockProductWithEffectiveScopedVoucher() {
        when(conversationStore.find(any())).thenReturn(Optional.empty());
        when(aiGateway.analyze(any(), any(), any())).thenReturn(Optional.empty());
        ChatCampaignOffer offer = offer();
        when(campaignOfferService.findEffectiveOffers(null)).thenReturn(List.of(offer));
        when(discoveryService.discover(any(), eq(true), eq(null), eq("session-1")))
                .thenReturn(List.of(candidate()));

        var response = service.reply(
                null,
                "session-1",
                new ChatMessageRequest(
                        null,
                        "Laptop bán chạy dưới 20 triệu có voucher",
                        null));

        assertThat(response.intents()).contains(ChatIntent.BEST_SELLER, ChatIntent.CAMPAIGN_OFFERS);
        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).availableStock()).isEqualTo(8);
        assertThat(response.products().get(0).voucher().code()).isEqualTo("SCHOOL10");
        assertThat(response.products().get(0).reason()).contains("bán chạy").contains("voucher");
        ArgumentCaptor<ChatSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ChatSearchCriteria.class);
        verify(discoveryService).discover(
                criteriaCaptor.capture(), eq(true), eq(null), eq("session-1"));
        assertThat(criteriaCaptor.getValue().offerOnly()).isTrue();
        assertThat(criteriaCaptor.getValue().productScopeIds()).containsExactly(10L);
        verify(conversationStore).save(any(ChatConversationSnapshot.class));
        verify(analyticsEventService).track(eq(null), eq("session-1"), any());
    }

    @Test
    void answersGreetingWithoutCallingAiOrProductDiscovery() {
        when(conversationStore.find(any())).thenReturn(Optional.empty());

        var response = service.reply(
                null,
                "session-1",
                new ChatMessageRequest(null, "Xin chào bạn", null));

        assertThat(response.intents()).containsExactly(ChatIntent.GREETING);
        assertThat(response.answer()).contains("Xin chào").contains("trợ lý mua sắm");
        assertThat(response.products()).isEmpty();
        assertThat(response.quickReplies()).containsExactly(
                "Sản phẩm bán chạy",
                "Tư vấn theo nhu cầu",
                "Sản phẩm đang có voucher");
        verifyNoInteractions(aiGateway, campaignOfferService, discoveryService);
        verify(conversationStore).save(any(ChatConversationSnapshot.class));
    }

    @Test
    void doesNotSuggestProductsWhenNoCampaignOfferIsEffective() {
        when(conversationStore.find(any())).thenReturn(Optional.empty());
        when(aiGateway.analyze(any(), any(), any())).thenReturn(Optional.empty());
        when(campaignOfferService.findEffectiveOffers(null)).thenReturn(List.of());

        var response = service.reply(
                null,
                "session-1",
                new ChatMessageRequest(null, "Sản phẩm có voucher", null));

        assertThat(response.products()).isEmpty();
        assertThat(response.answer()).contains("chưa có campaign");
        verify(discoveryService, never()).discover(any(), any(Boolean.class), any(), any());
    }

    @Test
    void requiresSessionForAnonymousVisitor() {
        assertThatThrownBy(() -> service.reply(
                null, null, new ChatMessageRequest(null, "Laptop bán chạy", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("X-Session-Id");
    }

    @Test
    void rejectsConversationOwnedByAnotherVisitor() {
        UUID conversationId = UUID.randomUUID();
        when(conversationStore.find(conversationId)).thenReturn(Optional.of(
                new ChatConversationSnapshot(conversationId, "s:other", List.of())));

        assertThatThrownBy(() -> service.reply(
                null,
                "session-1",
                new ChatMessageRequest(conversationId, "Laptop bán chạy", null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not belong");
    }

    private ChatCampaignOffer offer() {
        return new ChatCampaignOffer(
                1L,
                "Back to School",
                "Campaign",
                5L,
                "SCHOOL10",
                DiscountType.PERCENT,
                BigDecimal.TEN,
                new BigDecimal("1000000"),
                BigDecimal.ZERO,
                PromotionScopeType.PRODUCT,
                LocalDateTime.now().plusDays(10),
                Map.of(ScopeRefType.PRODUCT, Set.of(10L)));
    }

    private ChatProductCandidate candidate() {
        return new ChatProductCandidate(
                10L,
                "laptop-pro",
                "Laptop Pro",
                "Description",
                3L,
                "Laptop",
                "MarketBrand",
                null,
                new BigDecimal("18000000"),
                new BigDecimal("19500000"),
                8);
    }
}
