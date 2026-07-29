package com.training.marketplace.service;

import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.analytics.AnalyticsIngestionStatus;
import com.training.marketplace.dto.request.TrackAnalyticsEventBatchRequest;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.entity.AnalyticsEvent;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.service.impl.AnalyticsEventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventServiceTest {

    @Mock private AnalyticsEventRepository analyticsEventRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository productVariantRepository;

    private AnalyticsEventService analyticsEventService;

    @BeforeEach
    void setUp() {
        analyticsEventService = new AnalyticsEventServiceImpl(
                analyticsEventRepository,
                productRepository,
                productVariantRepository);
    }

    @Test
    void track_validProductView_persistsAcceptedEvent() {
        when(productRepository.existsById(10L)).thenReturn(true);
        when(analyticsEventRepository.existsByEventId("evt-1")).thenReturn(false);

        var response = analyticsEventService.track(7L, "session-1", productView("evt-1", 10L));

        assertThat(response.status()).isEqualTo(AnalyticsIngestionStatus.ACCEPTED);
        ArgumentCaptor<AnalyticsEvent> eventCaptor = ArgumentCaptor.forClass(AnalyticsEvent.class);
        verify(analyticsEventRepository).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventId()).isEqualTo("evt-1");
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo(AnalyticsEventType.PRODUCT_VIEW);
        assertThat(eventCaptor.getValue().getUserId()).isEqualTo(7L);
        assertThat(eventCaptor.getValue().getSessionId()).isEqualTo("session-1");
    }

    @Test
    void track_duplicateEventId_returnsDuplicateIgnoredWithoutSave() {
        when(productRepository.existsById(10L)).thenReturn(true);
        when(analyticsEventRepository.existsByEventId("evt-1")).thenReturn(true);

        var response = analyticsEventService.track(7L, "session-1", productView("evt-1", 10L));

        assertThat(response.status()).isEqualTo(AnalyticsIngestionStatus.DUPLICATE_IGNORED);
        verify(analyticsEventRepository, never()).save(any());
    }

    @Test
    void track_invalidSchemaVersion_returnsRejectedWithoutSave() {
        var request = new TrackAnalyticsEventRequest(
                "evt-2",
                "v2",
                AnalyticsEventType.PAGE_VIEW,
                LocalDateTime.now(),
                "web",
                "desktop",
                null,
                null,
                null,
                null,
                "/products",
                null,
                Map.of());

        var response = analyticsEventService.track(null, "session-1", request);

        assertThat(response.status()).isEqualTo(AnalyticsIngestionStatus.REJECTED);
        assertThat(response.message()).contains("schemaVersion");
        verify(analyticsEventRepository, never()).save(any());
    }

    @Test
    void trackBatch_returnsStatusSummary() {
        when(productRepository.existsById(10L)).thenReturn(true);
        when(productRepository.existsById(11L)).thenReturn(true);
        when(analyticsEventRepository.existsByEventId("accepted")).thenReturn(false);
        when(analyticsEventRepository.existsByEventId("duplicate")).thenReturn(true);

        var accepted = productView("accepted", 10L);
        var duplicate = productView("duplicate", 11L);
        var rejected = new TrackAnalyticsEventRequest(
                "rejected",
                "v1",
                AnalyticsEventType.SEARCH,
                LocalDateTime.now(),
                "web",
                null,
                null,
                null,
                null,
                null,
                null,
                " ",
                Map.of());

        var response = analyticsEventService.trackBatch(null, "session-1",
                new TrackAnalyticsEventBatchRequest(List.of(accepted, duplicate, rejected)));

        assertThat(response.accepted()).isEqualTo(1);
        assertThat(response.duplicateIgnored()).isEqualTo(1);
        assertThat(response.rejected()).isEqualTo(1);
        assertThat(response.events()).hasSize(3);
        verify(analyticsEventRepository).save(any(AnalyticsEvent.class));
    }

    private TrackAnalyticsEventRequest productView(String eventId, Long productId) {
        return new TrackAnalyticsEventRequest(
                eventId,
                "v1",
                AnalyticsEventType.PRODUCT_VIEW,
                LocalDateTime.now(),
                "web",
                "desktop",
                null,
                productId,
                null,
                null,
                null,
                null,
                Map.of("placement", "product-detail"));
    }
}
