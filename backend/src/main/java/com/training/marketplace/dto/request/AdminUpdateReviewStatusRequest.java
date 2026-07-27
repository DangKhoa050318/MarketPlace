package com.training.marketplace.dto.request;

import com.training.marketplace.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateReviewStatusRequest(
        @NotNull(message = "Review status is required")
        ReviewStatus status
) {}
