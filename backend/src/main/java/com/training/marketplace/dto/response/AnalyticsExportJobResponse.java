package com.training.marketplace.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.analytics.AnalyticsExportType;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnalyticsExportJobResponse(
        UUID id,
        AnalyticsExportType type,
        AnalyticsExportStatus status,
        String fileName,
        String downloadUrl,
        String errorMessage,
        Instant completedAt,
        Instant expiresAt
) {
}
