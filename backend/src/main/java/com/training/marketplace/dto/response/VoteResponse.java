package com.training.marketplace.dto.response;

import com.training.marketplace.enums.ContentType;

public record VoteResponse(
        ContentType targetType,
        Long targetId,
        Long helpfulCount,
        Boolean isVoted
) {}
