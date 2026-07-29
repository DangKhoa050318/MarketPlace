package com.training.marketplace.dto.response;

import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.analytics.AnalyticsIngestionStatus;

import java.time.LocalDateTime;

public record AnalyticsEventResponse(
        String eventId,
        AnalyticsEventType type,
        AnalyticsIngestionStatus status,
        String message,
        LocalDateTime receivedAt
) {
}
