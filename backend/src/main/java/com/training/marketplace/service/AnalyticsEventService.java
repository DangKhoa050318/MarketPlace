package com.training.marketplace.service;

import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.dto.response.AnalyticsEventResponse;

public interface AnalyticsEventService {
    AnalyticsEventResponse track(Long userId, String sessionId, TrackAnalyticsEventRequest request);
}
