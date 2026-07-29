package com.training.marketplace.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.training.marketplace.analytics.AnalyticsEventSource;
import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.analytics.RecommendationStrategyType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Schema(
        name = "TrackAnalyticsEventRequest",
        description = """
                Versioned canonical analytics event envelope. Identity is derived from JWT and
                X-Session-Id rather than accepted from the request body. Required fields that vary
                by event type are documented in docs/analytics-event-schema-v1.md.
                """)
public record TrackAnalyticsEventRequest(
        @Schema(
                description = "Client-generated idempotency identifier for this event",
                example = "cbe865ca-3c3c-4dc6-b5cb-a30833342848")
        @NotNull(message = "Event ID is required")
        UUID eventId,

        @Schema(description = "Analytics contract version", example = "1", defaultValue = "1")
        Integer schemaVersion,

        @NotNull(message = "Event type is required")
        @Schema(
                description = "Canonical analytics event type",
                example = "RECOMMENDATION_IMPRESSION")
        AnalyticsEventType type,

        @Schema(
                description = "Event timestamp in UTC using ISO-8601",
                example = "2026-07-28T12:30:00Z")
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @NotNull(message = "Event timestamp is required")
        Instant occurredAt,

        @Schema(description = "Product/SPU involved in the event", example = "12")
        @Positive(message = "Product ID must be positive")
        Long productId,

        @Schema(description = "Sellable variant/SKU involved in cart or purchase events", example = "25")
        @Positive(message = "Variant ID must be positive")
        Long variantId,

        @Schema(description = "Customer journey source", example = "RECOMMENDATION")
        AnalyticsEventSource source,

        @Schema(description = "Recommendation UI placement", example = "PRODUCT_DETAIL_SIMILAR")
        RecommendationPlacement placement,

        @Schema(
                description = "Identifier returned by the recommendation response",
                example = "93fc3727-47ae-4cbe-88ee-f9934753deca")
        UUID recommendationRequestId,

        @Schema(description = "Rule-based strategy that produced the recommendation", example = "SIMILAR")
        RecommendationStrategyType strategy,

        @Schema(description = "Zero-based item position inside the recommendation placement", example = "2")
        @PositiveOrZero(message = "Recommendation position cannot be negative")
        Integer position,

        @Schema(description = "SKU quantity for add-to-cart or purchase", example = "1")
        @Positive(message = "Quantity must be positive")
        Integer quantity,

        @Schema(description = "Order identifier for server-generated purchase events", example = "101")
        Long orderId,

        @Schema(description = "Trusted order-item price for server-generated purchase events", example = "499.99")
        BigDecimal unitPrice,

        @Schema(description = "Non-canonical extension metadata; must not contain PII")
        Map<String, Object> properties
) {
    public static final int CURRENT_SCHEMA_VERSION = 1;

    public TrackAnalyticsEventRequest {
        schemaVersion = schemaVersion == null ? CURRENT_SCHEMA_VERSION : schemaVersion;
        properties = properties == null ? Map.of() : properties;
    }

    /**
     * Compatibility constructor for existing internal callers while the public JSON contract
     * moves from the legacy generic-properties envelope to schema version 1.
     */
    public TrackAnalyticsEventRequest(
            AnalyticsEventType type,
            LocalDateTime occurredAt,
            Map<String, Object> properties) {
        this(
                null,
                CURRENT_SCHEMA_VERSION,
                type,
                occurredAt == null ? null : occurredAt.toInstant(ZoneOffset.UTC),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                properties);
    }
}
