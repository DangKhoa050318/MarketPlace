package com.training.marketplace.payment;

public class PaymentGatewayUnavailableException extends RuntimeException {
    public PaymentGatewayUnavailableException(String message) {
        super(message);
    }
}
