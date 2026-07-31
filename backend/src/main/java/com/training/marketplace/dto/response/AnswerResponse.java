package com.training.marketplace.dto.response;

import com.training.marketplace.enums.ModerationStatus;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long id,
        Long questionId,
        Long userId,
        String userName,
        String content,
        Boolean isOfficial,
        ModerationStatus status,
        Long helpfulCount,
        Boolean isVotedByCurrentUser,
        LocalDateTime createdAt
) {}
