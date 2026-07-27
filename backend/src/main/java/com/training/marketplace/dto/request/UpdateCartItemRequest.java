package com.training.marketplace.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity must not be negative")
        Integer quantity
) {}
