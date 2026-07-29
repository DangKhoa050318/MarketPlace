package com.training.marketplace.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.training.marketplace.analytics.AnalyticsIngestionStatus;
import com.training.marketplace.analytics.AnalyticsEventType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record AnalyticsEventResponse(
        AnalyticsEventType type,
        Long userId,
        String sessionId,
        LocalDateTime occurredAt,
        Map<String, Object> properties,
        UUID eventId,
        AnalyticsIngestionStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        Instant receivedAt
) {
    /**
     * Compatibility constructor for existing internal callers and tests.
     */
    public AnalyticsEventResponse(
            AnalyticsEventType type,
            Long userId,
            String sessionId,
            LocalDateTime occurredAt,
            Map<String, Object> properties) {
        this(type, userId, sessionId, occurredAt, properties, null, null, null);
    }
}
