package com.training.marketplace.service;

import com.training.marketplace.enums.PaymentMethod;

public record ChatCheckoutState(
        ChatCheckoutStage stage,
        String shippingAddress,
        String couponCode,
        PaymentMethod paymentMethod
) {
    public ChatCheckoutState(
            ChatCheckoutStage stage,
            String shippingAddress,
            String couponCode) {
        this(stage, shippingAddress, couponCode, null);
    }
}
