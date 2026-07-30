package com.training.marketplace.dto.response;

import java.time.Instant;

public record AnalyticsRetentionResponse(
        Instant cutoff,
        int eventsAnonymized,
        int aggregateRowsUpdated,
        int rawEventsDeleted
) {
}
