package com.training.marketplace.dto.response;

import java.util.List;

public record AnalyticsBatchIngestionResponse(
        int accepted,
        int duplicates,
        int total,
        List<AnalyticsEventResponse> events
) {
}
