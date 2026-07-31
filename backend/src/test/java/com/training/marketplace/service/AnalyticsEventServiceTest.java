package com.training.marketplace.service;

import com.training.marketplace.analytics.AnalyticsEventSource;
import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.analytics.AnalyticsIngestionStatus;
import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.dto.request.AnalyticsRetentionRequest;
import com.training.marketplace.dto.request.TrackAnalyticsEventBatchRequest;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.entity.AnalyticsEvent;
import com.training.marketplace.repository.AnalyticsEventIngestionRepository;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.service.impl.AnalyticsEventServiceImpl;
import com.training.marketplace.service.impl.AnalyticsEventValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventServiceTest {

    @Mock
    private AnalyticsEventRepository analyticsEventRepository;

    @Mock
    private AnalyticsEventIngestionRepository analyticsEventIngestionRepository;

    @Mock
    private AnalyticsEventValidator analyticsEventValidator;

    @InjectMocks
    private AnalyticsEventServiceImpl analyticsEventService;

    @Test
    void track_persistsTypedRecommendationContextAndReturnsAcceptedResponse() {
        UUID eventId = UUID.fromString("cbe865ca-3c3c-4dc6-b5cb-a30833342848");
        UUID requestId = UUID.fromString("93fc3727-47ae-4cbe-88ee-f9934753deca");
        Instant occurredAt = Instant.parse("2026-07-28T12:30:00Z");
        var request = new TrackAnalyticsEventRequest(
                eventId,
                1,
                AnalyticsEventType.RECOMMENDATION_IMPRESSION,
                occurredAt,
                10L,
                20L,
                AnalyticsEventSource.RECOMMENDATION,
                RecommendationPlacement.PRODUCT_DETAIL_SIMILAR,
                requestId,
                RecommendationStrategyType.SIMILAR,
                2,
                null,
                null,
                null,
                Map.of("surface", "product-detail"));
        when(analyticsEventIngestionRepository.insertIfAbsent(any())).thenReturn(true);

        var response = analyticsEventService.track(7L, "session-1", request);

        ArgumentCaptor<AnalyticsEvent> eventCaptor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventIngestionRepository).insertIfAbsent(eventCaptor.capture());
        AnalyticsEvent saved = eventCaptor.getValue();
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getSchemaVersion()).isEqualTo(1);
        assertThat(saved.getEventType()).isEqualTo(AnalyticsEventType.RECOMMENDATION_IMPRESSION);
        assertThat(saved.getUserId()).isEqualTo(7L);
        assertThat(saved.getSessionId()).isEqualTo("session-1");
        assertThat(saved.getProductId()).isEqualTo(10L);
        assertThat(saved.getVariantId()).isEqualTo(20L);
        assertThat(saved.getPlacement()).isEqualTo(RecommendationPlacement.PRODUCT_DETAIL_SIMILAR);
        assertThat(saved.getRecommendationRequestId()).isEqualTo(requestId);
        assertThat(saved.getOccurredAt()).isEqualTo(occurredAt);
        assertThat(saved.getReceivedAt()).isNotNull();

        assertThat(response.eventId()).isEqualTo(eventId);
        assertThat(response.status()).isEqualTo(AnalyticsIngestionStatus.ACCEPTED);
        assertThat(response.receivedAt()).isEqualTo(saved.getReceivedAt());
    }

    @Test
    void track_promotesLegacyCanonicalPropertiesWithoutBreakingExistingCallers() {
        var request = new TrackAnalyticsEventRequest(
                AnalyticsEventType.ADD_TO_CART,
                null,
                Map.of("productId", 10, "variantId", 20, "quantity", 2));
        when(analyticsEventIngestionRepository.insertIfAbsent(any())).thenReturn(true);

        var response = analyticsEventService.track(null, "anonymous-session", request);

        ArgumentCaptor<AnalyticsEvent> eventCaptor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventIngestionRepository).insertIfAbsent(eventCaptor.capture());
        AnalyticsEvent saved = eventCaptor.getValue();
        assertThat(saved.getEventId()).isNotNull();
        assertThat(saved.getUserId()).isNull();
        assertThat(saved.getProductId()).isEqualTo(10L);
        assertThat(saved.getVariantId()).isEqualTo(20L);
        assertThat(saved.getQuantity()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(AnalyticsIngestionStatus.ACCEPTED);
    }

    @Test
    void track_returnsDuplicateIgnoredWithoutOverwritingOriginalEvent() {
        UUID eventId = UUID.fromString("cbe865ca-3c3c-4dc6-b5cb-a30833342848");
        Instant originalReceivedAt = Instant.parse("2026-07-28T12:31:00Z");
        var request = new TrackAnalyticsEventRequest(
                eventId,
                1,
                AnalyticsEventType.PRODUCT_VIEW,
                Instant.parse("2026-07-28T12:30:00Z"),
                10L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of());
        AnalyticsEvent original = AnalyticsEvent.builder()
                .eventId(eventId)
                .schemaVersion(1)
                .eventType(AnalyticsEventType.PRODUCT_VIEW)
                .userId(7L)
                .sessionId("session-1")
                .productId(10L)
                .occurredAt(request.occurredAt())
                .receivedAt(originalReceivedAt)
                .properties(Map.of())
                .build();
        when(analyticsEventIngestionRepository.insertIfAbsent(any())).thenReturn(false);
        when(analyticsEventRepository.findByEventId(eventId)).thenReturn(Optional.of(original));

        var response = analyticsEventService.track(7L, "session-1", request);

        assertThat(response.eventId()).isEqualTo(eventId);
        assertThat(response.status()).isEqualTo(AnalyticsIngestionStatus.DUPLICATE_IGNORED);
        assertThat(response.receivedAt()).isEqualTo(originalReceivedAt);
        verify(analyticsEventRepository).findByEventId(eventId);
    }

    @Test
    void trackBatch_rejectsMoreThanMaximumEvents() {
        var events = java.util.stream.IntStream.range(0, 51)
                .mapToObj(i -> new TrackAnalyticsEventRequest(
                        UUID.randomUUID(),
                        1,
                        AnalyticsEventType.PAGE_VIEW,
                        Instant.parse("2026-07-28T12:30:00Z"),
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
                        Map.of()))
                .toList();

        assertThatThrownBy(() -> analyticsEventService.trackBatch(null, "session-1",
                new TrackAnalyticsEventBatchRequest(events)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("batch size");
    }

    @Test
    void trackBatch_countsAcceptedAndDuplicateStatuses() {
        UUID duplicateId = UUID.fromString("cbe865ca-3c3c-4dc6-b5cb-a30833342848");
        var accepted = new TrackAnalyticsEventRequest(
                UUID.randomUUID(), 1, AnalyticsEventType.PAGE_VIEW, Instant.parse("2026-07-28T12:30:00Z"),
                null, null, null, null, null, null, null, null, null, null, Map.of());
        var duplicate = new TrackAnalyticsEventRequest(
                duplicateId, 1, AnalyticsEventType.PAGE_VIEW, Instant.parse("2026-07-28T12:31:00Z"),
                null, null, null, null, null, null, null, null, null, null, Map.of());
        AnalyticsEvent original = AnalyticsEvent.builder()
                .eventId(duplicateId)
                .schemaVersion(1)
                .eventType(AnalyticsEventType.PAGE_VIEW)
                .sessionId("session-1")
                .occurredAt(duplicate.occurredAt())
                .receivedAt(Instant.parse("2026-07-28T12:32:00Z"))
                .properties(Map.of())
                .build();
        when(analyticsEventIngestionRepository.insertIfAbsent(any()))
                .thenReturn(true)
                .thenReturn(false);
        when(analyticsEventRepository.findByEventId(duplicateId)).thenReturn(Optional.of(original));

        var response = analyticsEventService.trackBatch(null, "session-1",
                new TrackAnalyticsEventBatchRequest(List.of(accepted, duplicate)));

        assertThat(response.total()).isEqualTo(2);
        assertThat(response.accepted()).isEqualTo(1);
        assertThat(response.duplicates()).isEqualTo(1);
    }

    @Test
    void anonymizeExpiredRawEvents_aggregatesBeforeAnonymizingAndDeletingRawEvents() {
        when(analyticsEventRepository.aggregateExpiredRawEvents(any())).thenReturn(2);
        when(analyticsEventRepository.anonymizeExpiredRawEvents(any())).thenReturn(3);
        when(analyticsEventRepository.deleteExpiredRawEvents(any())).thenReturn(5);

        var response = analyticsEventService.anonymizeExpiredRawEvents(new AnalyticsRetentionRequest(30));

        assertThat(response.eventsAnonymized()).isEqualTo(3);
        assertThat(response.aggregateRowsUpdated()).isEqualTo(2);
        assertThat(response.rawEventsDeleted()).isEqualTo(5);
        assertThat(response.cutoff()).isNotNull();
    }
}
