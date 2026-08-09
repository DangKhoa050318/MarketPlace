package com.training.marketplace.dto.request;

import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaygateWebhookRequest(
        String event,
        String transactionRef,
        Long merchantId,
        String orderId,
        BigDecimal amount,
        String status,
        String token
) {
    public PaygateWebhookRequest(
            String event,
            String transactionRef,
            Long merchantId,
            String orderId,
            BigDecimal amount,
            String status
    ) {
        this(event, transactionRef, merchantId, orderId, amount, status, null);
    }
}
