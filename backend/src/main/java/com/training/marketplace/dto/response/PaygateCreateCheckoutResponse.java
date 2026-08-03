package com.training.marketplace.dto.response;

public record PaygateCreateCheckoutResponse(
        Boolean success,
        String status,
        String message,
        PaygateCheckoutData data
) {
    public record PaygateCheckoutData(
            String token,
            String paymentUrl,
            String expiresAt
    ) {}
}
