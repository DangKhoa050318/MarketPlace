package com.training.marketplace.payment;

import java.math.BigDecimal;

public record PaymentRequest(
        String idempotencyKey,
        Long orderId,
        Long userId,
        String customerEmail,
        BigDecimal amount
) {}
