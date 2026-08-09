package com.training.marketplace.dto.response;

import com.training.marketplace.enums.ChatIntent;

import java.util.List;
import java.util.UUID;

public record ChatMessageResponse(
        UUID conversationId,
        UUID messageId,
        String answer,
        List<ChatIntent> intents,
        List<ChatProductCardResponse> products,
        List<String> quickReplies,
        UUID traceId
) {
    public ChatMessageResponse {
        intents = intents == null ? List.of() : List.copyOf(intents);
        products = products == null ? List.of() : List.copyOf(products);
        quickReplies = quickReplies == null ? List.of() : List.copyOf(quickReplies);
    }
}
