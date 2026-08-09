package com.training.marketplace.service;

import com.training.marketplace.dto.request.ChatPageContext;

import java.util.List;
import java.util.Optional;

public interface ChatAiGateway {
    Optional<ChatIntentAnalysis> analyze(
            String message,
            List<ChatHistoryMessage> history,
            ChatPageContext pageContext);
}
