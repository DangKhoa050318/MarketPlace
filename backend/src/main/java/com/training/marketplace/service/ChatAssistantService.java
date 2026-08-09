package com.training.marketplace.service;

import com.training.marketplace.dto.request.ChatMessageRequest;
import com.training.marketplace.dto.response.ChatMessageResponse;

public interface ChatAssistantService {
    ChatMessageResponse reply(
            Long userId,
            String sessionId,
            ChatMessageRequest request);
}
