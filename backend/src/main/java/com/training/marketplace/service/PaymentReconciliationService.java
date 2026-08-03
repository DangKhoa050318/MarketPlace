package com.training.marketplace.service;

import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.payment.PaymentResult;

public interface PaymentReconciliationService {

    boolean prepare(OrderCreatedEvent event);

    ReconciliationOutcome reconcile(OrderCreatedEvent event, PaymentResult result);

    void markRefunded(String eventId);

    enum ReconciliationOutcome {
        COMPLETED,
        REFUND_REQUIRED,
        ALREADY_COMPLETED
    }
}
