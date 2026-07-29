package com.training.marketplace.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TrackAnalyticsEventBatchRequest(
        @NotNull(message = "Events are required")
        @Size(min = 1, max = 50, message = "Analytics event batch size must be between 1 and 50")
        List<@Valid TrackAnalyticsEventRequest> events
) {
}
