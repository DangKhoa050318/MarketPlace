package com.training.marketplace.service;

import java.util.List;
import java.util.UUID;

public record ChatConversationSnapshot(
        UUID conversationId,
        String ownerKey,
        List<ChatHistoryMessage> messages,
        ChatCheckoutState checkoutState
) {
    public ChatConversationSnapshot {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    public ChatConversationSnapshot(
            UUID conversationId,
            String ownerKey,
            List<ChatHistoryMessage> messages) {
        this(conversationId, ownerKey, messages, null);
    }
}
