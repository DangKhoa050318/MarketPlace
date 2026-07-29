package com.training.marketplace.dto.request;

import com.training.marketplace.enums.ContentType;
import jakarta.validation.constraints.NotNull;

public record VoteRequest(
        @NotNull(message = "Target type is required")
        ContentType targetType,

        @NotNull(message = "Target ID is required")
        Long targetId
) {}
