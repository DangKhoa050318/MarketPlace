package com.training.marketplace.service;

import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.service.impl.PaymentWebhookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryFacade inventoryFacade;

    @InjectMocks
    private PaymentWebhookServiceImpl paymentWebhookService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentWebhookService, "merchantApiKey", "mock-merchant-api-key-123456");
    }

    @Test
    void processPaygateWebhook_Success_UpdatesOrderAndFulfillsStock() {
        // given
        Long orderId = 100L;
        Order order = new Order();
        order.setId(orderId);
        order.setWarehouseId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING_PAYGATE);

        OrderItem item = new OrderItem();
        item.setVariantId(55L);
        item.setQuantity(2);
        order.setItems(List.of(item));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-100",
                new BigDecimal("150000.00"),
                "SUCCESS"
        );

        // when
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(request, "mock-merchant-api-key-123456");

        // then
        assertThat(result.get("orderId")).isEqualTo(orderId);
        assertThat(result.get("status")).isEqualTo("CONFIRMED");
        assertThat(result.get("paymentStatus")).isEqualTo("PAID");
        assertThat(result.get("idempotent")).isEqualTo(false);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);

        verify(orderRepository).save(order);
        verify(inventoryFacade).fulfill(eq(1L), eq(Map.of(55L, 2)));
    }

    @Test
    void processPaygateWebhook_AlreadyPaid_ReturnsIdempotentNoDuplicateFulfill() {
        // given
        Long orderId = 101L;
        Order order = new Order();
        order.setId(orderId);
        order.setWarehouseId(1L);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(PaymentStatus.PAID);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-101",
                new BigDecimal("150000.00"),
                "SUCCESS"
        );

        // when
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(request, null);

        // then
        assertThat(result.get("idempotent")).isEqualTo(true);
        assertThat(result.get("paymentStatus")).isEqualTo("PAID");

        verify(orderRepository, never()).save(any());
        verify(inventoryFacade, never()).fulfill(any(), any());
    }

    @Test
    void processPaygateWebhook_OrderNotFound_ThrowsResourceNotFoundException() {
        // given
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-999",
                new BigDecimal("100.00"),
                "SUCCESS"
        );

        // when & then
        assertThatThrownBy(() -> paymentWebhookService.processPaygateWebhook(request, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    void processPaygateWebhook_InvalidSignature_ThrowsBadRequestException() {
        // given
        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-100",
                new BigDecimal("100.00"),
                "SUCCESS"
        );

        // when & then
        assertThatThrownBy(() -> paymentWebhookService.processPaygateWebhook(request, "invalid-bad-signature"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid webhook signature");
    }
}
