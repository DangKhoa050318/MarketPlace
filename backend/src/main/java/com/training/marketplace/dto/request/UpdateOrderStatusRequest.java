package com.training.marketplace.dto.request;

import com.training.marketplace.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "Order status is required")
        OrderStatus status,
        String note
) {
}
