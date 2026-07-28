package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.dto.response.AnalyticsEventResponse;
import com.training.marketplace.service.AnalyticsEventService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    @Override
    public AnalyticsEventResponse track(Long userId, String sessionId, TrackAnalyticsEventRequest request) {
        LocalDateTime occurredAt = request.occurredAt() != null ? request.occurredAt() : LocalDateTime.now();
        Map<String, Object> properties = request.properties() != null ? request.properties() : Map.of();
        AnalyticsEventResponse response = new AnalyticsEventResponse(
                request.type(),
                userId,
                sessionId,
                occurredAt,
                properties);

        log.info("Analytics event tracked: type={}, userId={}, sessionId={}, occurredAt={}, properties={}",
                response.type(), response.userId(), response.sessionId(), response.occurredAt(), response.properties());
        return response;
    }
}
