package com.training.marketplace.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateDeliveryRequest(
        @Size(max = 100, message = "Carrier must not exceed 100 characters")
        @Pattern(regexp = ".*\\S.*", message = "Carrier must not be blank")
        String carrier,

        @Size(max = 100, message = "Tracking code must not exceed 100 characters")
        @Pattern(regexp = ".*\\S.*", message = "Tracking code must not be blank")
        String trackingCode,

        @FutureOrPresent(message = "Estimated delivery date must be today or later")
        LocalDate estimatedDelivery
) {
}
