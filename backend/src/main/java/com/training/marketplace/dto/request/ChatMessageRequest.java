package com.training.marketplace.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatMessageRequest(
        UUID conversationId,

        @NotBlank(message = "Message cannot be empty")
        @Size(max = 500, message = "Message must not exceed 500 characters")
        String message,

        @Valid ChatPageContext pageContext,

        @Size(max = 50, message = "Coupon code must not exceed 50 characters")
        String couponCode
) {
    public ChatMessageRequest(
            UUID conversationId,
            String message,
            ChatPageContext pageContext) {
        this(conversationId, message, pageContext, null);
    }
}
