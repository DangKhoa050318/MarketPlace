package com.training.marketplace.dto.request;

import jakarta.validation.constraints.Min;

public record AnalyticsRetentionRequest(
        @Min(value = 1, message = "Raw event retention must be at least 1 day")
        Integer rawRetentionDays
) {
}
