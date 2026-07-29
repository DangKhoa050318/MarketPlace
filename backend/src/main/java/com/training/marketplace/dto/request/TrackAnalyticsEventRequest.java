package com.training.marketplace.dto.request;

import com.training.marketplace.analytics.AnalyticsEventType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

public record TrackAnalyticsEventRequest(
        @Schema(description = "Client-generated idempotency key for this event")
        String eventId,

        @Schema(description = "Analytics schema version", example = "v1")
        String schemaVersion,

        @Schema(description = "Canonical storefront event type")
        AnalyticsEventType type,

        @Schema(description = "Client-side event time")
        LocalDateTime occurredAt,

        String source,
        String deviceType,
        String campaign,
        Long productId,
        Long variantId,
        Integer quantity,
        String path,
        String query,
        Map<String, Object> properties
) {
}
