package com.training.marketplace.service;

import com.training.marketplace.dto.request.AnalyticsRetentionRequest;
import com.training.marketplace.dto.request.TrackAnalyticsEventBatchRequest;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.dto.response.AnalyticsBatchIngestionResponse;
import com.training.marketplace.dto.response.AnalyticsEventResponse;
import com.training.marketplace.dto.response.AnalyticsRetentionResponse;

public interface AnalyticsEventService {
    AnalyticsEventResponse track(Long userId, String sessionId, TrackAnalyticsEventRequest request);

    AnalyticsBatchIngestionResponse trackBatch(Long userId, String sessionId, TrackAnalyticsEventBatchRequest request);

    AnalyticsRetentionResponse anonymizeExpiredRawEvents(AnalyticsRetentionRequest request);
}
