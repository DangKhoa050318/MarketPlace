package com.training.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MergeAnonymousJourneyRequest(
        @NotBlank(message = "Session ID is required")
        @Size(max = 128, message = "Session ID must not exceed 128 characters")
        String sessionId
) {
}
