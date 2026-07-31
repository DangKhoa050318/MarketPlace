package com.training.marketplace.dto.response;

import com.training.marketplace.enums.PublishStatus;

import java.time.LocalDateTime;

public record BannerResponse(
        Long id,
        String title,
        String imageUrlDesktop,
        String imageUrlMobile,
        String altText,
        String targetUrl,
        String position,
        PublishStatus status,
        Integer displayOrder,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
