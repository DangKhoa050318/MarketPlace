package com.training.marketplace.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.config.CoreFeature;
import com.training.marketplace.config.CoreFeatureProperties;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Component
@ConditionalOnBean(CoreFeatureProperties.class)
@RequiredArgsConstructor
public class CoreFeatureRequestFilter extends OncePerRequestFilter {

    private final CoreFeatureProperties featureProperties;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        CoreFeature feature = featureFor(request.getRequestURI());
        if (feature == null) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!featureProperties.enabled(feature)) {
            meterRegistry.counter("marketplace.feature.disabled.requests",
                    "feature", metricName(feature)).increment();
            response.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(objectMapper.writeValueAsString(
                    ApiResponse.error("Feature is temporarily unavailable")));
            return;
        }

        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            filterChain.doFilter(request, response);
        } finally {
            sample.stop(meterRegistry.timer("marketplace.feature.http.requests",
                    "feature", metricName(feature),
                    "method", request.getMethod(),
                    "status", Integer.toString(response.getStatus())));
            log.info("feature_request feature={} method={} path={} status={}",
                    feature, request.getMethod(), request.getRequestURI(), response.getStatus());
        }
    }

    private CoreFeature featureFor(String path) {
        if (path.startsWith("/api/v1/recommendations")) {
            return CoreFeature.RECOMMENDATIONS;
        }
        if (path.startsWith("/api/v1/coupons")) {
            return CoreFeature.PROMOTIONS;
        }
        if (path.startsWith("/api/v1/reviews")
                || path.startsWith("/api/v1/questions")
                || path.startsWith("/api/v1/answers")
                || path.startsWith("/api/v1/content")
                || path.startsWith("/api/v1/admin/moderation")) {
            return CoreFeature.REVIEW_QA;
        }
        if (path.startsWith("/api/v1/analytics")
                || path.startsWith("/api/v1/admin/analytics")
                || path.startsWith("/api/v1/wishlist")
                || path.startsWith("/api/v1/anonymous-wishlist")
                || path.startsWith("/api/v1/recently-viewed")
                || path.startsWith("/api/v1/journey")) {
            return CoreFeature.JOURNEY_ANALYTICS;
        }
        return null;
    }

    private String metricName(CoreFeature feature) {
        return feature.name().toLowerCase(Locale.ROOT);
    }
}
