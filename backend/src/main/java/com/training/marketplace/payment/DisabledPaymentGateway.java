package com.training.marketplace.payment;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Safe default: an unconfigured deployment must never confirm an unpaid order. */
@Component
@ConditionalOnProperty(
        prefix = "marketplace.payment",
        name = "provider",
        havingValue = "disabled",
        matchIfMissing = true)
public class DisabledPaymentGateway implements PaymentGateway {

    @Override
    public PaymentResult charge(PaymentRequest request) {
        throw new PaymentGatewayUnavailableException(
                "Payment provider is disabled; configure marketplace.payment.provider");
    }

    @Override
    public void refund(String providerTransactionId, BigDecimal amount, String idempotencyKey) {
        throw new PaymentGatewayUnavailableException(
                "Payment provider is disabled; refund cannot be processed");
    }
}
