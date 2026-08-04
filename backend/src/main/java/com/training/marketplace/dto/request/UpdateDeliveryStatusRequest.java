package com.training.marketplace.dto.request;

import com.training.marketplace.enums.DeliveryEventType;
import com.training.marketplace.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateDeliveryStatusRequest(
        @NotNull(message = "Expected version is required")
        @PositiveOrZero(message = "Expected version must be zero or greater")
        Long expectedVersion,

        @NotNull(message = "Delivery status is required")
        DeliveryStatus status,

        DeliveryEventType eventType,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note
) {
}
