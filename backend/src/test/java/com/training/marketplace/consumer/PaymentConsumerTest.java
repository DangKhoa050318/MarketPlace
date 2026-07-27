package com.training.marketplace.consumer;

import com.training.marketplace.entity.Order;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentConsumerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private PaymentConsumer paymentConsumer;

    @Test
    void processPayment_validEvent_updatesOrderStatusToConfirmed() {
        // Given
        var item = new OrderCreatedEvent.OrderItemInfo(1L, "SKU-1", "Smartphone", BigDecimal.valueOf(499.99), 2, BigDecimal.valueOf(999.98));
        var event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                101L,
                5L,
                "user5@example.com",
                1L,
                BigDecimal.valueOf(999.98),
                LocalDateTime.now(),
                List.of(item)
        );

        Order order = Order.builder()
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(999.98))
                .shippingAddress("123 St")
                .build();
        order.setId(101L);

        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));

        // When
        paymentConsumer.processPayment(event);

        // Then
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        verify(orderRepository).save(order);
    }
}
