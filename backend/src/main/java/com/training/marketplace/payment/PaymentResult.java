package com.training.marketplace.payment;

public record PaymentResult(
        boolean approved,
        String providerTransactionId,
        String failureReason
) {
    public static PaymentResult approved(String providerTransactionId) {
        return new PaymentResult(true, providerTransactionId, null);
    }

    public static PaymentResult declined(String failureReason) {
        return new PaymentResult(false, null, failureReason);
    }
}
