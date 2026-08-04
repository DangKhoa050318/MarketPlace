package com.training.marketplace.dto.request;

import com.training.marketplace.enums.DeliveryEventType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AddDeliveryEventRequest(
        @NotNull(message = "Request ID is required")
        UUID requestId,

        @NotNull(message = "Event type is required")
        DeliveryEventType eventType,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note
) {
}
