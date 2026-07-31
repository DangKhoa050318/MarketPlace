package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductPerformanceResponse(
        Long productId,
        String productName,
        Long categoryId,
        long productViews,
        long wishlists,
        long addToCarts,
        long orders,
        BigDecimal averageRating,
        long questionCount,
        Instant lastUpdatedAt
) {
}
