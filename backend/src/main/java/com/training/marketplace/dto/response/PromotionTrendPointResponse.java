package com.training.marketplace.dto.response;

import java.time.LocalDate;

public record PromotionTrendPointResponse(
        LocalDate date,
        long impressions,
        long clicks,
        long addToCarts,
        long attributedOrders
) {
}
