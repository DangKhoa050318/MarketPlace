package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.ChatbotRequest;
import com.training.marketplace.dto.response.ChatbotResponse;
import com.training.marketplace.service.ChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<ChatbotResponse>> askQuestion(
            @Valid @RequestBody ChatbotRequest request) {
        ChatbotResponse response = chatbotService.askQuestion(request);
        return ResponseEntity.ok(ApiResponse.success("Success", response));
    }
}
