package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.ChatPageContext;
import com.training.marketplace.enums.ChatIntent;
import com.training.marketplace.service.ChatHistoryMessage;
import com.training.marketplace.service.ChatIntentAnalysis;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedChatIntentAnalyzer {

    private static final Pattern MAX_PRICE = Pattern.compile(
            "(?:duoi|toi da|khong qua|tam|khoang)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|m|nghin|k)?");
    private static final Pattern MIN_PRICE = Pattern.compile(
            "(?:tren|toi thieu|tu)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|m|nghin|k)?");
    private static final Pattern RANGE_PRICE = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*(?:-|den|toi)\\s*(\\d+(?:[.,]\\d+)?)\\s*(trieu|tr|m|nghin|k)");
    private static final Pattern PRICE_PHRASE = Pattern.compile(
            "(?i)(?:duoi|tren|tu|toi da|toi thieu|khong qua|khoang|tam)?\\s*"
                    + "\\d+(?:[.,]\\d+)?\\s*(?:trieu|tr|m|nghin|ngan|k)(?:\\s*(?:-|den|toi)\\s*"
                    + "\\d+(?:[.,]\\d+)?\\s*(?:trieu|tr|m|nghin|ngan|k)?)?");
    private static final Pattern GREETING = Pattern.compile(
            "\\b(?:xin chao|chao ban|chao|hello|hi|hey|alo)\\b");
    private static final Pattern THANKS = Pattern.compile(
            "\\b(?:cam on|thank you|thanks|thank|tks)\\b");
    private static final Pattern HELP = Pattern.compile(
            "\\b(?:ban co the giup gi|ban giup duoc gi|ban lam duoc gi|giup toi voi|"
                    + "giup minh voi|tro giup|help)\\b");
    private static final Pattern CONVERSATION_FILLERS = Pattern.compile(
            "\\b(?:ban|shop|nhe|nha|a|oi|minh|toi|em|anh|chi|rat|nhieu|xin|vui long|"
                    + "voi|duoc|co|the|gi|lam|me)\\b");

    public ChatIntentAnalysis analyze(
            String message,
            List<ChatHistoryMessage> history,
            ChatPageContext pageContext) {
        ChatIntent conversationalIntent = conversationalIntent(normalize(message));
        if (conversationalIntent != null) {
            return new ChatIntentAnalysis(
                    List.of(conversationalIntent),
                    null,
                    null,
                    null,
                    null,
                    null,
                    Map.of(),
                    false,
                    null);
        }

        String historyContext = recentUserContext(history);
        String combined = historyContext + " " + message;
        String normalized = normalize(combined);
        Set<ChatIntent> intents = new LinkedHashSet<>();

        if (containsAny(normalized, "ban chay", "best seller", "bestseller", "pho bien", "top ", "hot")) {
            intents.add(ChatIntent.BEST_SELLER);
        }
        if (containsAny(normalized, "voucher", "coupon", "ma giam", "giam gia", "khuyen mai",
                "campaign", "uu dai")) {
            intents.add(ChatIntent.CAMPAIGN_OFFERS);
        }
        if (intents.isEmpty() || containsAny(normalized, "goi y", "tu van", "can mua", "phu hop", "tim")) {
            intents.add(ChatIntent.PRODUCT_DISCOVERY);
        }

        PriceRange currentPrice = extractPrices(normalize(message));
        PriceRange historyPrice = extractPrices(normalize(historyContext));
        BigDecimal minPrice = currentPrice.hasConstraint() ? currentPrice.minPrice() : historyPrice.minPrice();
        BigDecimal maxPrice = currentPrice.hasConstraint() ? currentPrice.maxPrice() : historyPrice.maxPrice();

        String query = cleanupQuery(message);
        if (query == null || query.isBlank()) {
            query = recentUserQuery(history);
        }
        boolean noExplicitNeed = (query == null || query.isBlank())
                && minPrice == null
                && maxPrice == null
                && (pageContext == null || pageContext.categoryId() == null)
                && intents.equals(Set.of(ChatIntent.PRODUCT_DISCOVERY));

        return new ChatIntentAnalysis(
                new ArrayList<>(intents),
                blankToNull(query),
                null,
                minPrice,
                maxPrice,
                null,
                Map.of(),
                noExplicitNeed,
                noExplicitNeed
                        ? "Bạn đang tìm dòng sản phẩm nào và ngân sách dự kiến là bao nhiêu?"
                        : null);
    }

    private String recentUserContext(List<ChatHistoryMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        return history.stream()
                .filter(item -> "user".equalsIgnoreCase(item.role()))
                .skip(Math.max(0, history.stream().filter(item -> "user".equalsIgnoreCase(item.role())).count() - 3))
                .map(ChatHistoryMessage::content)
                .reduce("", (left, right) -> left + " " + right);
    }

    private BigDecimal extractPrice(String normalized, Pattern pattern) {
        Matcher matcher = pattern.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        String unit = matcher.group(2);
        return decimal(matcher.group(1)).multiply(multiplier(unit == null ? "trieu" : unit));
    }

    private PriceRange extractPrices(String normalized) {
        BigDecimal minPrice = extractPrice(normalized, MIN_PRICE);
        BigDecimal maxPrice = extractPrice(normalized, MAX_PRICE);
        Matcher range = RANGE_PRICE.matcher(normalized);
        if (range.find()) {
            BigDecimal rangeMultiplier = multiplier(range.group(3));
            minPrice = decimal(range.group(1)).multiply(rangeMultiplier);
            maxPrice = decimal(range.group(2)).multiply(rangeMultiplier);
        }
        return new PriceRange(minPrice, maxPrice);
    }

    private String recentUserQuery(List<ChatHistoryMessage> history) {
        if (history == null) {
            return null;
        }
        for (int index = history.size() - 1; index >= 0; index--) {
            ChatHistoryMessage item = history.get(index);
            if (!"user".equalsIgnoreCase(item.role())) {
                continue;
            }
            String query = cleanupQuery(item.content());
            if (query != null && !query.isBlank()) {
                return query;
            }
        }
        return null;
    }

    private BigDecimal decimal(String value) {
        return new BigDecimal(value.replace(',', '.'));
    }

    private BigDecimal multiplier(String unit) {
        if (unit == null) {
            return BigDecimal.ONE;
        }
        return switch (unit.toLowerCase(Locale.ROOT)) {
            case "trieu", "tr", "m" -> BigDecimal.valueOf(1_000_000);
            case "nghin", "k" -> BigDecimal.valueOf(1_000);
            default -> BigDecimal.ONE;
        };
    }

    private String cleanupQuery(String message) {
        String cleaned = PRICE_PHRASE.matcher(normalize(message)).replaceAll(" ");
        cleaned = cleaned.replaceAll("(?i)\\b(toi|minh|em|can|muon|mua|tim|goi y|tu van|cho|giup|hay|san pham|"
                + "ban chay|pho bien|voucher|coupon|ma giam gia|giam gia|khuyen mai|campaign|uu dai|dang|co|voi|va|"
                + "xin chao|chao ban|chao|hello|hi|hey|alo|cam on|thank you|thanks|tks)\\b", " ");
        cleaned = cleaned.replaceAll("[?,.!]+", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() < 2 ? null : cleaned;
    }

    private ChatIntent conversationalIntent(String normalizedMessage) {
        if (isConversationalOnly(normalizedMessage, GREETING)) {
            return ChatIntent.GREETING;
        }
        if (isConversationalOnly(normalizedMessage, THANKS)) {
            return ChatIntent.THANKS;
        }
        if (isConversationalOnly(normalizedMessage, HELP)) {
            return ChatIntent.HELP;
        }
        return null;
    }

    private boolean isConversationalOnly(String message, Pattern intentPattern) {
        if (!intentPattern.matcher(message).find()) {
            return false;
        }
        String remainder = intentPattern.matcher(message).replaceAll(" ");
        remainder = CONVERSATION_FILLERS.matcher(remainder).replaceAll(" ");
        return remainder.replaceAll("[^a-z0-9]+", " ").isBlank();
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}", "")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            String term = needle.trim();
            Pattern wholeTerm = Pattern.compile(
                    "(?<![a-z0-9])" + Pattern.quote(term) + "(?![a-z0-9])");
            if (wholeTerm.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        private boolean hasConstraint() {
            return minPrice != null || maxPrice != null;
        }
    }
}
