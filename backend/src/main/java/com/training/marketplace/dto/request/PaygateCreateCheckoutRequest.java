package com.training.marketplace.dto.request;

import java.math.BigDecimal;

public record PaygateCreateCheckoutRequest(
        String apiKey,
        String orderId,
        BigDecimal amount,
        String description,
        String paymentMethod,
        String returnUrl,
        String cancelUrl
) {
    public PaygateCreateCheckoutRequest(String apiKey, String orderId, BigDecimal amount, String description, String returnUrl, String cancelUrl) {
        this(apiKey, orderId, amount, description, "PAYGATE", returnUrl, cancelUrl);
    }
}
