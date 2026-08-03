package com.training.marketplace.dto.response;

import com.training.marketplace.enums.PublishStatus;

import java.time.LocalDateTime;
import java.util.List;

public record CollectionResponse(
        Long id,
        String name,
        String slug,
        String description,
        PublishStatus status,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CollectionItemResponse> items
) {}
