package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.ChatbotRequest;
import com.training.marketplace.dto.response.ChatbotResponse;
import com.training.marketplace.service.ChatbotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "AI Chatbot assistant endpoints")
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    @Operation(summary = "Ask a question to AI chatbot")
    public ApiResponse<ChatbotResponse> askQuestion(
            @Valid @RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.askQuestion(request);
        return ApiResponse.success("Success", response);
    }
}

