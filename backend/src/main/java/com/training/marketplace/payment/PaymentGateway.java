package com.training.marketplace.payment;

import java.math.BigDecimal;

public interface PaymentGateway {

    /** Charge exactly once for a given request idempotency key. */
    PaymentResult charge(PaymentRequest request);

    /** Refund exactly once for a given refund idempotency key. */
    void refund(String providerTransactionId, BigDecimal amount, String idempotencyKey);
}
