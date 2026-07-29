package com.training.marketplace.dto.response;

import com.training.marketplace.enums.ModerationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record QuestionResponse(
        Long id,
        Long productId,
        Long userId,
        String userName,
        String content,
        ModerationStatus status,
        Long helpfulCount,
        Boolean isVotedByCurrentUser,
        List<AnswerResponse> answers,
        LocalDateTime createdAt
) {}
