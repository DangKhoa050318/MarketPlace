package com.training.marketplace.dto.request;

import com.training.marketplace.enums.ContentType;
import com.training.marketplace.enums.ModerationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateModerationStatusRequest(
        @NotNull(message = "Target type is required")
        ContentType targetType,

        @NotNull(message = "Target ID is required")
        Long targetId,

        @NotNull(message = "New status is required")
        ModerationStatus newStatus,

        String reason
) {}
