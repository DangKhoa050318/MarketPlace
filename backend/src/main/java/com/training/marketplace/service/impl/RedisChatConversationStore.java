package com.training.marketplace.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.config.ChatAssistantProperties;
import com.training.marketplace.service.ChatConversationSnapshot;
import com.training.marketplace.service.ChatConversationStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisChatConversationStore implements ChatConversationStore {

    private static final String KEY_PREFIX = "chat:conversation:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final ChatAssistantProperties properties;

    @Override
    public Optional<ChatConversationSnapshot> find(UUID conversationId) {
        if (conversationId == null) {
            return Optional.empty();
        }
        try {
            Object value = redisTemplate.opsForValue().get(key(conversationId));
            if (value == null) {
                return Optional.empty();
            }
            if (value instanceof ChatConversationSnapshot snapshot) {
                return Optional.of(snapshot);
            }
            return Optional.of(objectMapper.convertValue(value, ChatConversationSnapshot.class));
        } catch (RuntimeException exception) {
            log.warn("Unable to read chat conversation {} from Redis", conversationId, exception);
            return Optional.empty();
        }
    }

    @Override
    public void save(ChatConversationSnapshot conversation) {
        try {
            redisTemplate.opsForValue().set(
                    key(conversation.conversationId()),
                    conversation,
                    properties.getConversationTtl());
        } catch (RuntimeException exception) {
            log.warn("Unable to persist chat conversation {} to Redis", conversation.conversationId(), exception);
        }
    }

    private String key(UUID conversationId) {
        return KEY_PREFIX + conversationId;
    }
}
