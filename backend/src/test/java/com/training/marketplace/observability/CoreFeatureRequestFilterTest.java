package com.training.marketplace.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.config.CoreFeatureProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CoreFeatureRequestFilterTest {

    @Test
    void disabledJourneyAnalytics_returnsServiceUnavailableAndMetric() throws Exception {
        CoreFeatureProperties properties = new CoreFeatureProperties();
        properties.setJourneyAnalytics(false);
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CoreFeatureRequestFilter filter = new CoreFeatureRequestFilter(
                properties, registry, new ObjectMapper().findAndRegisterModules());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/admin/analytics/overview");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("Disabled feature must not reach the controller");
        });

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("temporarily unavailable");
        assertThat(registry.get("marketplace.feature.disabled.requests")
                .tag("feature", "journey_analytics").counter().count()).isEqualTo(1);
    }

    @Test
    void enabledRecommendation_recordsRequestTimer() throws Exception {
        CoreFeatureProperties properties = new CoreFeatureProperties();
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        CoreFeatureRequestFilter filter = new CoreFeatureRequestFilter(
                properties, registry, new ObjectMapper().findAndRegisterModules());
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/api/v1/recommendations");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) ->
                ((jakarta.servlet.http.HttpServletResponse) res).setStatus(200));

        assertThat(registry.get("marketplace.feature.http.requests")
                .tag("feature", "recommendations")
                .tag("method", "GET")
                .tag("status", "200")
                .timer().count()).isEqualTo(1);
    }
}
