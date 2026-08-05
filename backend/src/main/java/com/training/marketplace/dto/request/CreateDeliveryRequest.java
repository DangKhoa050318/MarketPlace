package com.training.marketplace.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateDeliveryRequest(
        @NotBlank(message = "Carrier is required")
        @Size(max = 100, message = "Carrier must not exceed 100 characters")
        String carrier,

        @NotBlank(message = "Tracking code is required")
        @Size(max = 100, message = "Tracking code must not exceed 100 characters")
        String trackingCode,

        @NotNull(message = "Estimated delivery date is required")
        @FutureOrPresent(message = "Estimated delivery date must be today or later")
        LocalDate estimatedDelivery
) {
}
