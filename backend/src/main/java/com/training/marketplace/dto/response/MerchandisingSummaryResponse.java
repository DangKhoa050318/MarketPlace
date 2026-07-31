package com.training.marketplace.dto.response;

import com.training.marketplace.enums.MerchandisingTargetType;

/** Effectiveness of one merchandising target over a time range (B-408). CTR = clicks / impressions. */
public record MerchandisingSummaryResponse(
        MerchandisingTargetType targetType,
        Long targetId,
        long impressions,
        long clicks,
        double ctr,
        long attributedOrders
) {}
