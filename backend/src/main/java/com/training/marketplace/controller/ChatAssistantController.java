package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.ChatMessageRequest;
import com.training.marketplace.dto.response.ChatMessageResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.service.ChatAssistantService;
import com.training.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(
        name = "Chat Assistant",
        description = "Shopping discovery, campaign voucher, cart command and COD checkout assistant")
public class ChatAssistantController {

    private final ChatAssistantService chatAssistantService;
    private final UserService userService;

    @PostMapping("/messages")
    @Operation(
            summary = "Send a message to the shopping assistant",
            description = "Supports product discovery and a stateful authenticated COD checkout flow. "
                    + "Pass the same conversationId for payment method, address and optional note steps.")
    public ApiResponse<ChatMessageResponse> reply(
            Authentication authentication,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody ChatMessageRequest request) {
        return ApiResponse.success("Chat response generated", chatAssistantService.reply(
                currentUserId(authentication), sessionId, request));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            User user = userService.getAuthenticatedUser(authentication);
            return user == null ? null : user.getId();
        } catch (ResourceNotFoundException exception) {
            return null;
        }
    }
}
