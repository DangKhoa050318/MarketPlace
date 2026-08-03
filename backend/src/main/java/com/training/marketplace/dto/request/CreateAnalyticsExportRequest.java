package com.training.marketplace.dto.request;

import com.training.marketplace.analytics.AnalyticsExportType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateAnalyticsExportRequest(
        @NotNull AnalyticsExportType type,
        @NotNull Instant from,
        @NotNull Instant to,
        Long categoryId,
        Long productId,
        String campaign,
        String placement,
        String deviceType
) {
}
