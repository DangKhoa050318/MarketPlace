package com.training.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReviewReplyRequest(
        @NotBlank(message = "Reply cannot be empty")
        @Size(max = 1000, message = "Reply cannot exceed 1000 characters")
        String reply
) {}
