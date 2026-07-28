package com.training.marketplace.dto.response;

import com.training.marketplace.analytics.AnalyticsEventType;

import java.time.LocalDateTime;
import java.util.Map;

public record AnalyticsEventResponse(
        AnalyticsEventType type,
        Long userId,
        String sessionId,
        LocalDateTime occurredAt,
        Map<String, Object> properties
) {
}
