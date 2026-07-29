package com.training.marketplace.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.entity.AnalyticsEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

/**
 * PostgreSQL-specific append operation used to make event-id deduplication atomic.
 */
@Repository
@RequiredArgsConstructor
public class AnalyticsEventIngestionRepository {

    private static final String INSERT_SQL = """
            INSERT INTO analytics_events (
                event_id, schema_version, event_type, user_id, session_id, product_id, variant_id,
                source, placement, recommendation_request_id, strategy, position, quantity,
                order_id, unit_price, occurred_at, received_at, properties
            ) VALUES (
                :eventId, :schemaVersion, :eventType, :userId, :sessionId, :productId, :variantId,
                :source, :placement, :recommendationRequestId, :strategy, :position, :quantity,
                :orderId, :unitPrice, :occurredAt, :receivedAt, CAST(:properties AS jsonb)
            )
            ON CONFLICT (event_id) DO NOTHING
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public boolean insertIfAbsent(AnalyticsEvent event) {
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventId", event.getEventId())
                .addValue("schemaVersion", event.getSchemaVersion())
                .addValue("eventType", event.getEventType().name())
                .addValue("userId", event.getUserId())
                .addValue("sessionId", event.getSessionId())
                .addValue("productId", event.getProductId())
                .addValue("variantId", event.getVariantId())
                .addValue("source", enumName(event.getSource()))
                .addValue("placement", enumName(event.getPlacement()))
                .addValue("recommendationRequestId", event.getRecommendationRequestId())
                .addValue("strategy", enumName(event.getStrategy()))
                .addValue("position", event.getPosition())
                .addValue("quantity", event.getQuantity())
                .addValue("orderId", event.getOrderId())
                .addValue("unitPrice", event.getUnitPrice())
                .addValue("occurredAt", Timestamp.from(event.getOccurredAt()))
                .addValue("receivedAt", Timestamp.from(event.getReceivedAt()))
                .addValue("properties", serializeProperties(event));
        return jdbcTemplate.update(INSERT_SQL, parameters) == 1;
    }

    private String serializeProperties(AnalyticsEvent event) {
        try {
            return objectMapper.writeValueAsString(event.getProperties());
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Analytics event properties are not JSON serializable", exception);
        }
    }

    private String enumName(Enum<?> value) {
        return value == null ? null : value.name();
    }
}
