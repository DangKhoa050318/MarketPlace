package com.training.marketplace.dto.request;

import java.math.BigDecimal;

public record PaygateRefundRequest(
        String apiKey,
        String transactionRef,
        String orderId,
        BigDecimal amount,
        String reason
) {
}
