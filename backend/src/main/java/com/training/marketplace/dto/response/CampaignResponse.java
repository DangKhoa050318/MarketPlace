package com.training.marketplace.dto.response;

import com.training.marketplace.enums.CampaignStatus;

import java.time.LocalDateTime;

public record CampaignResponse(
        Long id,
        String name,
        String description,
        CampaignStatus status,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        Long promotionCodeId,
        String couponCode,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
