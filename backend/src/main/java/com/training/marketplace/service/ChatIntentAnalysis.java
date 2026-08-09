package com.training.marketplace.service;

import com.training.marketplace.enums.ChatIntent;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record ChatIntentAnalysis(
        List<ChatIntent> intents,
        String query,
        String category,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String brand,
        Map<String, String> attributes,
        boolean needsClarification,
        String clarificationQuestion
) {
    public ChatIntentAnalysis {
        intents = intents == null || intents.isEmpty()
                ? List.of(ChatIntent.PRODUCT_DISCOVERY) : List.copyOf(intents);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
