package com.training.marketplace.dto.response;

import java.util.List;

public record AnalyticsEventBatchResponse(
        int accepted,
        int duplicateIgnored,
        int rejected,
        List<AnalyticsEventResponse> events
) {
}
