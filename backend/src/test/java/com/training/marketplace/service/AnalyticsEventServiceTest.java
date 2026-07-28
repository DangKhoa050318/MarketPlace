package com.training.marketplace.service;

import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.service.impl.AnalyticsEventServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsEventServiceTest {

    private final AnalyticsEventService analyticsEventService = new AnalyticsEventServiceImpl();

    @Test
    void track_returnsCanonicalEventResponse() {
        var request = new TrackAnalyticsEventRequest(
                AnalyticsEventType.PRODUCT_VIEW,
                null,
                Map.of("productId", 10L));

        var response = analyticsEventService.track(7L, "session-1", request);

        assertThat(response.type()).isEqualTo(AnalyticsEventType.PRODUCT_VIEW);
        assertThat(response.userId()).isEqualTo(7L);
        assertThat(response.sessionId()).isEqualTo("session-1");
        assertThat(response.occurredAt()).isNotNull();
        assertThat(response.properties()).containsEntry("productId", 10L);
    }
}
