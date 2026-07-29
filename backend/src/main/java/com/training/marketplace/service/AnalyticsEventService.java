package com.training.marketplace.service;

import com.training.marketplace.dto.request.TrackAnalyticsEventBatchRequest;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.dto.response.AnalyticsEventBatchResponse;
import com.training.marketplace.dto.response.AnalyticsEventResponse;

public interface AnalyticsEventService {
    AnalyticsEventResponse track(Long userId, String sessionId, TrackAnalyticsEventRequest request);

    AnalyticsEventBatchResponse trackBatch(Long userId, String sessionId, TrackAnalyticsEventBatchRequest request);
}
