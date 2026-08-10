package com.training.marketplace.service;

public record ChatCheckoutState(
        ChatCheckoutStage stage,
        String shippingAddress,
        String couponCode
) {
}
