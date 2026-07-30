package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record AnalyticsDashboardFilter(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant from,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Instant to,
        Long categoryId,
        Long productId,
        String campaign,
        String placement,
        String deviceType
) {
}
