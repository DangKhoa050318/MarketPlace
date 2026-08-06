package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.service.InventoryFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Webhook replay idempotency — phantom-stock guard. PayGate retries the same webhook on network
 * errors, so a CANCELLED/FAILED replay must release reserved stock at most once, and a SUCCESS
 * replay must mark the order paid without touching stock. Uses real Order/OrderItem so the status transition set by the
 * first call is visible to the replays (that transition is exactly what the idempotency guard reads).
 */
@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private InventoryFacade inventoryFacade;

    @InjectMocks
    private PaymentWebhookServiceImpl service;

    private Order orderWith(Long id, OrderStatus status, PaymentStatus paymentStatus) {
        OrderItem item = OrderItem.builder().variantId(10L).quantity(1).build();
        return Order.builder()
                .id(id)
                .status(status)
                .paymentStatus(paymentStatus)
                .warehouseId(1L)
                .items(List.of(item))
                .build();
    }

    private PaygateWebhookRequest cancelled(String orderId) {
        return new PaygateWebhookRequest("PAYMENT_CANCELLED", "TX-" + orderId, 1L, orderId,
                new BigDecimal("100.00"), "CANCELLED");
    }

    private PaygateWebhookRequest success(String orderId) {
        return new PaygateWebhookRequest("PAYMENT_COMPLETED", "TX-" + orderId, 1L, orderId,
                new BigDecimal("100.00"), "SUCCESS");
    }

    @Test
    void cancelledWebhookReplayedThreeTimes_releasesStockExactlyOnce() {
        Order order = orderWith(1L, OrderStatus.PROCESSING, PaymentStatus.UNPAID);
        given(orderRepository.findByIdForUpdate(1L)).willReturn(Optional.of(order));

        service.processPaygateWebhook(cancelled("1"), null); // first: cancels + releases
        service.processPaygateWebhook(cancelled("1"), null); // retry -> idempotent no-op
        service.processPaygateWebhook(cancelled("1"), null); // retry -> idempotent no-op

        verify(inventoryFacade, times(1)).release(eq(1L), anyMap());
        verify(inventoryFacade, never()).fulfill(eq(1L), anyMap());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void successWebhookReplayedThreeTimes_marksPaidWithoutFulfillingStock() {
        Order order = orderWith(2L, OrderStatus.PENDING, PaymentStatus.UNPAID);
        given(orderRepository.findByIdForUpdate(2L)).willReturn(Optional.of(order));

        service.processPaygateWebhook(success("2"), null); // first: confirms + marks paid
        service.processPaygateWebhook(success("2"), null); // retry -> idempotent no-op
        service.processPaygateWebhook(success("2"), null); // retry -> idempotent no-op

        verify(inventoryFacade, never()).fulfill(eq(1L), anyMap());
        verify(inventoryFacade, never()).release(eq(1L), anyMap());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaygateTransactionRef()).isEqualTo("TX-2");
    }

    @Test
    void cancelledWebhookOnAlreadyPaidOrder_neverReleases() {
        // Out-of-order delivery: a stray CANCELLED webhook after the order was already PAID+fulfilled
        // must not release a fulfilled order — the broadened guard treats PAID as terminal too.
        Order order = orderWith(3L, OrderStatus.CONFIRMED, PaymentStatus.PAID);
        given(orderRepository.findByIdForUpdate(3L)).willReturn(Optional.of(order));

        Map<String, Object> result = service.processPaygateWebhook(cancelled("3"), null);

        verify(inventoryFacade, never()).release(eq(1L), anyMap());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result).containsEntry("idempotent", true);
    }

    @Test
    void cancelledSignalledByEventOnly_stillReleasesStock() {
        // User-cancels-checkout flow: cancellation arrives via the event field (PAYMENT_CANCELLED)
        // while status is not a cancel value. The merged branch detects it from `event` too, so the
        // reservation is released. A status-only condition would miss this and leave stock stuck.
        Order order = orderWith(4L, OrderStatus.PROCESSING, PaymentStatus.UNPAID);
        given(orderRepository.findByIdForUpdate(4L)).willReturn(Optional.of(order));

        PaygateWebhookRequest eventOnly = new PaygateWebhookRequest(
                "PAYMENT_CANCELLED", "TX-4", 1L, "4", new BigDecimal("100.00"), "PENDING");

        service.processPaygateWebhook(eventOnly, null);

        verify(inventoryFacade, times(1)).release(eq(1L), anyMap());
        verify(inventoryFacade, never()).fulfill(eq(1L), anyMap());
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }
}
