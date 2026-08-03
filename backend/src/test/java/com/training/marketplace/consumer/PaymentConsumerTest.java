package com.training.marketplace.consumer;

import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.payment.PaymentGateway;
import com.training.marketplace.payment.PaymentRequest;
import com.training.marketplace.payment.PaymentResult;
import com.training.marketplace.service.PaymentReconciliationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentConsumerTest {

    @Mock private PaymentGateway paymentGateway;
    @Mock private PaymentReconciliationService reconciliationService;
    @InjectMocks private PaymentConsumer paymentConsumer;

    private OrderCreatedEvent event;

    @BeforeEach
    void setUp() {
        event = new OrderCreatedEvent(
                "event-1", 101L, 5L, "user5@example.com", 1L,
                BigDecimal.valueOf(999.98), LocalDateTime.now(),
                List.of(new OrderCreatedEvent.OrderItemInfo(
                        1L, "SKU-1", "Smartphone", BigDecimal.valueOf(499.99),
                        2, BigDecimal.valueOf(999.98))));
    }

    @Test
    void processPayment_approved_reconcilesOrder() {
        PaymentResult result = PaymentResult.approved("txn-1");
        when(reconciliationService.prepare(event)).thenReturn(true);
        when(paymentGateway.charge(any(PaymentRequest.class))).thenReturn(result);
        when(reconciliationService.reconcile(event, result))
                .thenReturn(PaymentReconciliationService.ReconciliationOutcome.COMPLETED);

        paymentConsumer.processPayment(event);

        verify(reconciliationService).reconcile(event, result);
        verify(paymentGateway, never()).refund(any(), any(), any());
    }

    @Test
    void processPayment_duplicateTerminalEvent_doesNotCallGatewayAgain() {
        when(reconciliationService.prepare(event)).thenReturn(false);

        paymentConsumer.processPayment(event);

        verify(paymentGateway, never()).charge(any());
    }

    @Test
    void processPayment_cancelledAfterApproval_refundsIdempotently() {
        PaymentResult result = PaymentResult.approved("txn-1");
        when(reconciliationService.prepare(event)).thenReturn(true);
        when(paymentGateway.charge(any(PaymentRequest.class))).thenReturn(result);
        when(reconciliationService.reconcile(event, result))
                .thenReturn(PaymentReconciliationService.ReconciliationOutcome.REFUND_REQUIRED);

        paymentConsumer.processPayment(event);

        verify(paymentGateway).refund("txn-1", event.totalAmount(), "event-1:refund");
        verify(reconciliationService).markRefunded("event-1");
    }

    @Test
    void processPayment_gatewayOutagePropagatesForRabbitRetry() {
        when(reconciliationService.prepare(event)).thenReturn(true);
        when(paymentGateway.charge(any(PaymentRequest.class)))
                .thenThrow(new IllegalStateException("gateway unavailable"));

        assertThatThrownBy(() -> paymentConsumer.processPayment(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("gateway unavailable");

        verify(reconciliationService, never()).reconcile(any(), any());
    }
}
