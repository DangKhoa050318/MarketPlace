package com.training.marketplace.service;

import java.util.Optional;
import java.util.UUID;

public interface ChatConversationStore {
    Optional<ChatConversationSnapshot> find(UUID conversationId);
    void save(ChatConversationSnapshot conversation);
}
