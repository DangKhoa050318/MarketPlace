package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record PromotionRecommendationPerformanceResponse(
        String campaign,
        String placement,
        String strategy,
        long impressions,
        long clicks,
        BigDecimal clickThroughRate,
        long addToCarts,
        long attributedOrders,
        Instant lastUpdatedAt
) {
}
