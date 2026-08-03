package com.training.marketplace.dto.request;

import java.math.BigDecimal;

public record PaygateCreateCheckoutRequest(
        String apiKey,
        String orderId,
        BigDecimal amount,
        String description,
        String returnUrl,
        String cancelUrl
) {
}
