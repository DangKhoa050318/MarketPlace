package com.training.marketplace.service;

import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.exception.ForbiddenException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.service.impl.PaymentWebhookServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentWebhookServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private InventoryFacade inventoryFacade;

    private PaymentWebhookServiceImpl paymentWebhookService;

    @BeforeEach
    void setUp() {
        paymentWebhookService = new PaymentWebhookServiceImpl(orderRepository, inventoryFacade);
        ReflectionTestUtils.setField(paymentWebhookService, "merchantApiKey", "mock-merchant-api-key-123456");
    }

    @Test
    void processPaygateWebhook_Success_UpdatesOrderWithoutFulfillingStock() {
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

        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        String rawPayload = "{\"event\":\"PAYMENT_COMPLETED\",\"transactionRef\":\"TXN_998877\",\"merchantId\":1,\"orderId\":\"ORD-100\",\"amount\":150000.00,\"status\":\"SUCCESS\"}";
        String signature = com.training.marketplace.utils.HmacUtils.generateSignature(rawPayload, "mock-merchant-api-key-123456");

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-100",
                new BigDecimal("150000.00"),
                "SUCCESS"
        );

        // when
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(request, signature, rawPayload);

        // then
        assertThat(result.get("orderId")).isEqualTo(orderId);
        assertThat(result.get("status")).isEqualTo("CONFIRMED");
        assertThat(result.get("paymentStatus")).isEqualTo("PAID");
        assertThat(result.get("idempotent")).isEqualTo(false);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaygateTransactionRef()).isEqualTo("TXN_998877");

        verify(orderRepository).save(order);
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

        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-101",
                new BigDecimal("150000.00"),
                "SUCCESS"
        );

        // when
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(request, null, null);

        // then
        assertThat(result.get("idempotent")).isEqualTo(true);
        assertThat(result.get("paymentStatus")).isEqualTo("PAID");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void processPaygateWebhook_OrderNotFound_ThrowsResourceNotFoundException() {
        // given
        when(orderRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-999",
                new BigDecimal("100.00"),
                "SUCCESS"
        );

        // when & then
        assertThatThrownBy(() -> paymentWebhookService.processPaygateWebhook(request, null, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Order");
    }

    @Test
    void processPaygateWebhook_InvalidSignature_ThrowsForbiddenException() {
        // given
        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-100",
                new BigDecimal("100.00"),
                "SUCCESS"
        );

        // when & then — a wrong secret is an auth failure (403), and never touches the order/stock
        assertThatThrownBy(() -> paymentWebhookService.processPaygateWebhook(request, "invalid-bad-signature", "rawBody"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Invalid webhook signature");
        verifyNoInteractions(orderRepository, inventoryFacade);
    }

    @Test
    void processPaygateWebhook_RawApiKeyAsSignature_IsRejected() {
        // MP-C1 fix: Passing the raw merchant API key directly as X-Signature without HMAC hashing must be rejected.
        ReflectionTestUtils.setField(paymentWebhookService, "requireSignature", true);
        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED", "TXN_1", 1L, "ORD-100", new BigDecimal("100.00"), "SUCCESS");
        String rawPayload = "{\"event\":\"PAYMENT_COMPLETED\",\"orderId\":\"ORD-100\"}";

        assertThatThrownBy(() -> paymentWebhookService.processPaygateWebhook(
                request,
                "mock-merchant-api-key-123456",
                rawPayload
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Invalid webhook signature");
        verifyNoInteractions(orderRepository, inventoryFacade);
    }

    @Test
    void verifySignature_withApiKeyAsSignature_shouldNotPass() {
        // Mentor code-review test case requirement:
        // Passing raw merchant API key as X-Signature must return false / throw ForbiddenException
        ReflectionTestUtils.setField(paymentWebhookService, "requireSignature", true);
        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED", "TXN_1", 1L, "ORD-100", new BigDecimal("100.00"), "SUCCESS");
        String rawPayload = "{\"event\":\"PAYMENT_COMPLETED\",\"orderId\":\"ORD-100\"}";

        assertThatThrownBy(() -> paymentWebhookService.processPaygateWebhook(
                request,
                "mock-merchant-api-key-123456",
                rawPayload
        ))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void processPaygateWebhook_MissingSignature_WhenEnforced_ThrowsForbidden() {
        // The endpoint is public, so a webhook with no signature must be rejected when enforcement is on.
        ReflectionTestUtils.setField(paymentWebhookService, "requireSignature", true);
        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED", "TXN_1", 1L, "ORD-100", new BigDecimal("100.00"), "SUCCESS");

        assertThatThrownBy(() -> paymentWebhookService.processPaygateWebhook(request, null, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Missing webhook signature");
        verifyNoInteractions(orderRepository, inventoryFacade);
    }

    @Test
    void processPaygateWebhook_SignatureMerelyContainingKey_IsRejected() {
        // Old bug: String.contains() let "prefix-<key>-suffix" through. Exact constant-time match closes it.
        ReflectionTestUtils.setField(paymentWebhookService, "requireSignature", true);
        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED", "TXN_1", 1L, "ORD-100", new BigDecimal("100.00"), "SUCCESS");

        assertThatThrownBy(() -> paymentWebhookService.processPaygateWebhook(
                request,
                "some-junk" + "mock-merchant-api-key-123456" + "more-junk",
                "rawPayload"
        ))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Invalid webhook signature");
        verifyNoInteractions(orderRepository, inventoryFacade);
    }

    @Test
    void processPaygateWebhook_ValidSignature_WhenEnforced_Processes() {
        // Enforcement on + valid HMAC signature -> the webhook is authenticated and marks payment paid.
        ReflectionTestUtils.setField(paymentWebhookService, "requireSignature", true);
        Long orderId = 200L;
        Order order = new Order();
        order.setId(orderId);
        order.setWarehouseId(1L);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING_PAYGATE);
        OrderItem item = new OrderItem();
        item.setVariantId(7L);
        item.setQuantity(1);
        order.setItems(List.of(item));
        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        String rawPayload = "{\"event\":\"PAYMENT_COMPLETED\",\"transactionRef\":\"TXN_1\",\"merchantId\":1,\"orderId\":\"ORD-200\",\"amount\":100.00,\"status\":\"SUCCESS\"}";
        String signature = com.training.marketplace.utils.HmacUtils.generateSignature(rawPayload, "mock-merchant-api-key-123456");

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED", "TXN_1", 1L, "ORD-200", new BigDecimal("100.00"), "SUCCESS");

        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(
                request,
                signature,
                rawPayload
        );
        assertThat(result.get("status")).isEqualTo("CONFIRMED");
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(order.getPaygateTransactionRef()).isEqualTo("TXN_1");
        verify(inventoryFacade, never()).fulfill(eq(1L), eq(Map.of(7L, 1)));
    }

    @Test
    void processPaygateWebhook_AlreadyCancelled_ReturnsIdempotentNoDuplicateRelease() {
        // given
        Long orderId = 102L;
        Order order = new Order();
        order.setId(orderId);
        order.setWarehouseId(1L);
        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.UNPAID);

        when(orderRepository.findByIdForUpdate(orderId)).thenReturn(Optional.of(order));

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_CANCELLED",
                "TXN_998877",
                1L,
                "ORD-102",
                new BigDecimal("150000.00"),
                "CANCELLED"
        );

        // when
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(request, null, null);

        // then
        assertThat(result.get("idempotent")).isEqualTo(true);
        assertThat(result.get("status")).isEqualTo("CANCELLED");

        verify(orderRepository, never()).save(any());
        verify(inventoryFacade, never()).release(any(), any());
    }
}
