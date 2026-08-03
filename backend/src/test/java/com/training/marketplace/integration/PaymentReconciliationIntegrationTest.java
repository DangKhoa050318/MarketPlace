package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.consumer.PaymentConsumer;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.PaymentAttempt;
import com.training.marketplace.entity.StockLevel;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentAttemptStatus;
import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.payment.PaymentGateway;
import com.training.marketplace.payment.PaymentRequest;
import com.training.marketplace.payment.PaymentResult;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.PaymentAttemptRepository;
import com.training.marketplace.repository.StockLevelRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.InventoryFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestPropertySource(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class PaymentReconciliationIntegrationTest extends BaseIntegrationTest {

    @Autowired private PaymentConsumer paymentConsumer;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PaymentAttemptRepository paymentAttemptRepository;
    @Autowired private StockLevelRepository stockLevelRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private InventoryFacade inventoryFacade;

    @MockBean private PaymentGateway paymentGateway;

    @Test
    @Transactional
    void approvedPaymentIsConfirmedOnceWhenRabbitMessageIsRetried() {
        StockLevel stock = stockLevelRepository.findAll().get(0);
        int reservedBefore = stock.getReservedQuantity();
        Order order = createReservedOrder(stock, 2);
        OrderCreatedEvent event = eventFor(order, stock, 2);
        when(paymentGateway.charge(any(PaymentRequest.class))).thenReturn(PaymentResult.approved("txn-success"));

        paymentConsumer.processPayment(event);
        paymentConsumer.processPayment(event);

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
        assertThat(stockLevelRepository.findById(stock.getId()).orElseThrow().getReservedQuantity())
                .isEqualTo(reservedBefore + 2);
        PaymentAttempt attempt = paymentAttemptRepository.findByEventId(event.eventId()).orElseThrow();
        assertThat(attempt.getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        assertThat(attempt.getProviderTransactionId()).isEqualTo("txn-success");
        verify(paymentGateway, times(1)).charge(any(PaymentRequest.class));
    }

    @Test
    @Transactional
    void declinedPaymentCancelsOrderAndReleasesReservationExactlyOnce() {
        StockLevel stock = stockLevelRepository.findAll().get(0);
        int reservedBefore = stock.getReservedQuantity();
        Order order = createReservedOrder(stock, 3);
        OrderCreatedEvent event = eventFor(order, stock, 3);
        when(paymentGateway.charge(any(PaymentRequest.class))).thenReturn(PaymentResult.declined("declined"));

        paymentConsumer.processPayment(event);
        paymentConsumer.processPayment(event);

        assertThat(orderRepository.findById(order.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CANCELLED);
        assertThat(stockLevelRepository.findById(stock.getId()).orElseThrow().getReservedQuantity())
                .isEqualTo(reservedBefore);
        assertThat(paymentAttemptRepository.findByEventId(event.eventId()).orElseThrow().getStatus())
                .isEqualTo(PaymentAttemptStatus.DECLINED);
        verify(paymentGateway, times(1)).charge(any(PaymentRequest.class));
    }

    private Order createReservedOrder(StockLevel stock, int quantity) {
        inventoryFacade.reserve(stock.getWarehouseId(), Map.of(stock.getVariantId(), quantity));
        User user = userRepository.findAll().get(0);
        BigDecimal unitPrice = BigDecimal.valueOf(100);
        Order order = Order.builder()
                .user(user)
                .warehouseId(stock.getWarehouseId())
                .status(OrderStatus.PENDING)
                .totalAmount(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .discountAmount(BigDecimal.ZERO)
                .shippingAddress("Integration test address")
                .build();
        order.addItem(OrderItem.builder()
                .variantId(stock.getVariantId())
                .sku("TEST-SKU")
                .productName("Test product")
                .variantName("Test variant")
                .unitPrice(unitPrice)
                .quantity(quantity)
                .subtotal(unitPrice.multiply(BigDecimal.valueOf(quantity)))
                .build());
        return orderRepository.saveAndFlush(order);
    }

    private OrderCreatedEvent eventFor(Order order, StockLevel stock, int quantity) {
        OrderItem item = order.getItems().get(0);
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                order.getId(),
                order.getUser().getId(),
                order.getUser().getEmail(),
                stock.getWarehouseId(),
                order.getTotalAmount(),
                LocalDateTime.now(),
                List.of(new OrderCreatedEvent.OrderItemInfo(
                        item.getVariantId(), item.getSku(), item.getProductName(),
                        item.getUnitPrice(), quantity, item.getSubtotal())));
    }
}
