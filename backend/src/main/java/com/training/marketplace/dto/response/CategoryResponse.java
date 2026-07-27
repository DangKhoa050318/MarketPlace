package com.training.marketplace.dto.response;

import java.time.LocalDateTime;

public record CategoryResponse(
        Long id,
        String name,
        String code,
        String slug,
        Long parentId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
