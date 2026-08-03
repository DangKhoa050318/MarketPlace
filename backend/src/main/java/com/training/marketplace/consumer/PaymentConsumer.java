package com.training.marketplace.consumer;

import com.training.marketplace.config.RabbitMQConfig;
import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.payment.PaymentGateway;
import com.training.marketplace.payment.PaymentRequest;
import com.training.marketplace.payment.PaymentResult;
import com.training.marketplace.service.PaymentReconciliationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentConsumer {

    private final PaymentGateway paymentGateway;
    private final PaymentReconciliationService reconciliationService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_QUEUE)
    public void processPayment(OrderCreatedEvent event) {
        log.info("Received Payment Event: eventId={}, orderId={}, userId={}, totalAmount={}",
                event.eventId(), event.orderId(), event.userId(), event.totalAmount());

        if (!reconciliationService.prepare(event)) {
            log.info("Payment event {} was already reconciled; skipping", event.eventId());
            return;
        }

        PaymentResult result = paymentGateway.charge(new PaymentRequest(
                event.eventId(), event.orderId(), event.userId(), event.userEmail(), event.totalAmount()));
        PaymentReconciliationService.ReconciliationOutcome outcome =
                reconciliationService.reconcile(event, result);

        if (outcome == PaymentReconciliationService.ReconciliationOutcome.REFUND_REQUIRED) {
            paymentGateway.refund(
                    result.providerTransactionId(), event.totalAmount(), event.eventId() + ":refund");
            reconciliationService.markRefunded(event.eventId());
        }
    }
}
