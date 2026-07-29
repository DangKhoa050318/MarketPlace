package com.training.marketplace.dto.response;

import com.training.marketplace.enums.ContentType;
import com.training.marketplace.enums.ModerationStatus;

import java.time.LocalDateTime;

public record ModerationItemResponse(
        Long id,
        ContentType targetType,
        Long targetId,
        Long productId,
        String productName,
        Long authorId,
        String authorName,
        String content,
        ModerationStatus status,
        LocalDateTime createdAt
) {}
