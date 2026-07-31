package com.training.marketplace.dto.response;

import com.training.marketplace.enums.ContentType;
import com.training.marketplace.enums.ModerationStatus;

import java.time.LocalDateTime;

public record ModerationAuditLogResponse(
        Long id,
        ContentType targetType,
        Long targetId,
        Long moderatorId,
        String moderatorName,
        ModerationStatus oldStatus,
        ModerationStatus newStatus,
        String reason,
        LocalDateTime createdAt
) {}
