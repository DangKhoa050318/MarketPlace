package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TrackAnalyticsEventBatchRequest(
        @Schema(description = "Analytics events submitted in one ingestion call. Maximum 50.")
        @NotEmpty(message = "Events are required")
        List<@Valid TrackAnalyticsEventRequest> events
) {
}
