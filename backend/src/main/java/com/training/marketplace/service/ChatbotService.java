package com.training.marketplace.service;

import com.training.marketplace.dto.request.ChatbotRequest;
import com.training.marketplace.dto.response.ChatbotResponse;

public interface ChatbotService {
    /**
     * Ask the AI chatbot a question, supplying marketplace context automatically.
     * @param request the user's message and chat history
     * @return the AI's text response
     */
    ChatbotResponse askQuestion(ChatbotRequest request);
}
