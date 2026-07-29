package com.training.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateOrderRequest(
        @NotBlank(message = "Shipping address is required") String shippingAddress,
        String note,
        String couponCode   // optional coupon applied at checkout (FEATURE-STP-02)
) {}
