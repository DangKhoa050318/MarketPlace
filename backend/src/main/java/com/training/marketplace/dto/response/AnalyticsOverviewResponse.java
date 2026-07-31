package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record AnalyticsOverviewResponse(
        long productViews,
        long addToCarts,
        long beginCheckouts,
        long orders,
        BigDecimal addToCartRate,
        BigDecimal checkoutRate,
        BigDecimal orderConversionRate,
        BigDecimal returningCustomerRate,
        Instant lastUpdatedAt
) {
}
