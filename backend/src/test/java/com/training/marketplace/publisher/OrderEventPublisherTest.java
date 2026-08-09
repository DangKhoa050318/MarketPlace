package com.training.marketplace.publisher;

import com.training.marketplace.config.RabbitMQConfig;
import com.training.marketplace.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderEventPublisher orderEventPublisher;

    @Test
    void publishOrderCreatedEvent_validEvent_sendsToExchangeAndRoutingKey() {
        // Given
        var item = new OrderCreatedEvent.OrderItemInfo(1L, "SKU-1", "Laptop", BigDecimal.valueOf(999.99), 1, BigDecimal.valueOf(999.99));
        var event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                100L,
                1L,
                "user@example.com",
                1L,
                BigDecimal.valueOf(999.99),
                LocalDateTime.now(),
                List.of(item)
        );

        // Initialize transaction context for testing
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            // When
            orderEventPublisher.publishOrderCreatedEvent(event);

            // Trigger afterCommit manually to simulate successful transaction commit
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);

            // Then
            verify(rabbitTemplate).convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                    event
            );
        } finally {
            // Clean up
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
