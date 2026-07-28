package com.training.marketplace.dto.request;

import com.training.marketplace.analytics.AnalyticsEventType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

public record TrackAnalyticsEventRequest(
        @NotNull(message = "Event type is required")
        @Schema(description = "Canonical analytics event type")
        AnalyticsEventType type,

        @Schema(description = "Client supplied event timestamp")
        LocalDateTime occurredAt,

        @Schema(description = "Canonical event properties such as path, productId, query, variantId, orderId, totalAmount")
        Map<String, Object> properties
) {
}
