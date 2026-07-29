package com.training.marketplace.dto;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.training.marketplace.analytics.AnalyticsEventSource;
import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsEventSchemaTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
            .findAndAddModules()
            .build();

    @Test
    void eventType_containsAllRecommendationFoundationEvents() {
        Set<String> eventTypes = Arrays.stream(AnalyticsEventType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertThat(eventTypes).contains(
                "PRODUCT_VIEW",
                "RECOMMENDATION_IMPRESSION",
                "RECOMMENDATION_CLICK",
                "ADD_TO_CART",
                "PURCHASE");
    }

    @Test
    void recommendationImpression_roundTripsWithTypedVersionedContext() throws Exception {
        UUID eventId = UUID.fromString("cbe865ca-3c3c-4dc6-b5cb-a30833342848");
        UUID requestId = UUID.fromString("93fc3727-47ae-4cbe-88ee-f9934753deca");
        Instant occurredAt = Instant.parse("2026-07-28T12:30:00Z");
        var request = new TrackAnalyticsEventRequest(
                eventId,
                1,
                AnalyticsEventType.RECOMMENDATION_IMPRESSION,
                occurredAt,
                12L,
                25L,
                AnalyticsEventSource.RECOMMENDATION,
                RecommendationPlacement.PRODUCT_DETAIL_SIMILAR,
                requestId,
                RecommendationStrategyType.SIMILAR,
                2,
                null,
                null,
                null,
                Map.of("surface", "product-detail"));

        String json = objectMapper.writeValueAsString(request);
        TrackAnalyticsEventRequest restored =
                objectMapper.readValue(json, TrackAnalyticsEventRequest.class);

        assertThat(restored).isEqualTo(request);
        assertThat(json).contains(
                "\"schemaVersion\":1",
                "\"occurredAt\":\"2026-07-28T12:30:00Z\"",
                "\"source\":\"RECOMMENDATION\"",
                "\"placement\":\"PRODUCT_DETAIL_SIMILAR\"",
                "\"strategy\":\"SIMILAR\"");
    }

    @Test
    void legacyConstructor_preservesExistingInternalCallersAndUsesUtc() {
        var request = new TrackAnalyticsEventRequest(
                AnalyticsEventType.PRODUCT_VIEW,
                LocalDateTime.of(2026, 7, 28, 12, 30),
                Map.of("productId", 12L));

        assertThat(request.schemaVersion())
                .isEqualTo(TrackAnalyticsEventRequest.CURRENT_SCHEMA_VERSION);
        assertThat(request.occurredAt()).isEqualTo(Instant.parse("2026-07-28T12:30:00Z"));
        assertThat(request.properties()).containsEntry("productId", 12L);
    }
}
