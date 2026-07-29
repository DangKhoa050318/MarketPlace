package com.training.marketplace.entity;

import com.training.marketplace.analytics.AnalyticsEventSource;
import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.analytics.RecommendationStrategyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Append-only raw analytics event. User, product, and variant references are stored as scalar
 * identifiers so ingestion never loads or exposes JPA entities at the API boundary.
 */
@Entity
@Table(name = "analytics_events")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "schema_version", nullable = false)
    private Integer schemaVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AnalyticsEventType eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "session_id", length = 128)
    private String sessionId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "variant_id")
    private Long variantId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private AnalyticsEventSource source;

    @Enumerated(EnumType.STRING)
    @Column(length = 80)
    private RecommendationPlacement placement;

    @Column(name = "recommendation_request_id")
    private UUID recommendationRequestId;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private RecommendationStrategyType strategy;

    @Column
    private Integer position;

    @Column
    private Integer quantity;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> properties = Map.of();
}
