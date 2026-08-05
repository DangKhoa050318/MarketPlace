package com.training.marketplace.dto.request;

import java.math.BigDecimal;

public record PaygateWebhookRequest(
        String event,
        String transactionRef,
        Long merchantId,
        String orderId,
        BigDecimal amount,
        String status
) {
}
