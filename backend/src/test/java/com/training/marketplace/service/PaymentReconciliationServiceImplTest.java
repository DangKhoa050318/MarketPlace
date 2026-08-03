package com.training.marketplace.service;

import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.PaymentAttempt;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentAttemptStatus;
import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.payment.PaymentResult;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.PaymentAttemptRepository;
import com.training.marketplace.service.impl.PaymentReconciliationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReconciliationServiceImplTest {

    @Mock private PaymentAttemptRepository paymentAttemptRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private InventoryFacade inventoryFacade;
    @Mock private PromotionService promotionService;
    @InjectMocks private PaymentReconciliationServiceImpl service;

    private OrderCreatedEvent event;
    private Order order;
    private PaymentAttempt attempt;

    @BeforeEach
    void setUp() {
        event = new OrderCreatedEvent(
                "event-1", 10L, 2L, "customer@example.com", 3L,
                BigDecimal.valueOf(200), LocalDateTime.now(), List.of());
        order = Order.builder()
                .warehouseId(3L)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200))
                .shippingAddress("address")
                .promotionCodeId(7L)
                .build();
        order.setId(10L);
        order.addItem(OrderItem.builder().variantId(99L).quantity(2).build());
        attempt = PaymentAttempt.builder()
                .eventId("event-1")
                .orderId(10L)
                .amount(BigDecimal.valueOf(200))
                .status(PaymentAttemptStatus.PROCESSING)
                .build();
    }

    @Test
    void reconcile_declined_releasesReservationAndCancelsAtomically() {
        when(paymentAttemptRepository.findByEventIdForUpdate("event-1")).thenReturn(Optional.of(attempt));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        service.reconcile(event, PaymentResult.declined("card declined"));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.DECLINED);
        verify(inventoryFacade).release(3L, Map.of(99L, 2));
        verify(promotionService).refundIfPresent(7L, 10L);
        verify(orderRepository).save(order);
    }

    @Test
    void reconcile_terminalAttempt_isIdempotent() {
        attempt.setStatus(PaymentAttemptStatus.DECLINED);
        when(paymentAttemptRepository.findByEventIdForUpdate("event-1")).thenReturn(Optional.of(attempt));

        PaymentReconciliationService.ReconciliationOutcome outcome =
                service.reconcile(event, PaymentResult.declined("card declined"));

        assertThat(outcome).isEqualTo(PaymentReconciliationService.ReconciliationOutcome.ALREADY_COMPLETED);
        verify(orderRepository, never()).findByIdForUpdate(10L);
        verify(inventoryFacade, never()).release(3L, Map.of(99L, 2));
    }

    @Test
    void reconcile_orderCancelledAfterApproval_requiresRefund() {
        order.setStatus(OrderStatus.CANCELLED);
        when(paymentAttemptRepository.findByEventIdForUpdate("event-1")).thenReturn(Optional.of(attempt));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(order));

        PaymentReconciliationService.ReconciliationOutcome outcome =
                service.reconcile(event, PaymentResult.approved("txn-1"));

        assertThat(outcome).isEqualTo(PaymentReconciliationService.ReconciliationOutcome.REFUND_REQUIRED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.REFUND_REQUIRED);
        assertThat(attempt.getProviderTransactionId()).isEqualTo("txn-1");
    }

    @Test
    void prepare_reusedEventIdWithDifferentOrderDataIsRejected() {
        attempt.setOrderId(999L);
        when(paymentAttemptRepository.findByEventIdForUpdate("event-1")).thenReturn(Optional.of(attempt));

        assertThatThrownBy(() -> service.prepare(event))
                .isInstanceOf(com.training.marketplace.exception.BadRequestException.class)
                .hasMessageContaining("reused with different order data");
    }
}
