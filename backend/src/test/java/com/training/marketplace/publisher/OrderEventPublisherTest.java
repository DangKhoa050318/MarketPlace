package com.training.marketplace.publisher;

import com.training.marketplace.config.RabbitMQConfig;
import com.training.marketplace.event.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OrderEventPublisher orderEventPublisher;

    private OrderCreatedEvent createTestEvent() {
        var item = new OrderCreatedEvent.OrderItemInfo(1L, "SKU-1", "Laptop", BigDecimal.valueOf(999.99), 1, BigDecimal.valueOf(999.99));
        return new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                100L,
                1L,
                "user@example.com",
                1L,
                BigDecimal.valueOf(999.99),
                LocalDateTime.now(),
                List.of(item)
        );
    }

    @Test
    @DisplayName("publishOrderCreatedEvent: throws IllegalStateException when transaction synchronization is not active")
    void publishOrderCreatedEvent_withoutTransaction_throwsIllegalStateException() {
        OrderCreatedEvent event = createTestEvent();

        assertThatThrownBy(() -> orderEventPublisher.publishOrderCreatedEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transaction synchronization is not active");

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(Object.class));
    }

    @Test
    @DisplayName("publishOrderCreatedEvent: registers synchronization and sends event only after commit")
    void publishOrderCreatedEvent_withActiveTransaction_publishesOnlyAfterCommit() {
        OrderCreatedEvent event = createTestEvent();

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            orderEventPublisher.publishOrderCreatedEvent(event);

            // Verify event is not sent prior to transaction commit
            verify(rabbitTemplate, never()).convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                    event
            );

            // Simulate successful commit
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            // Verify event sent after commit
            verify(rabbitTemplate).convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                    event
            );
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("publishOrderCreatedEvent: event is not published after transaction rollback")
    void publishOrderCreatedEvent_onRollback_doesNotPublishEvent() {
        OrderCreatedEvent event = createTestEvent();

        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            orderEventPublisher.publishOrderCreatedEvent(event);

            // Simulate rollback (no afterCommit triggered, just clear)
            TransactionSynchronizationManager.clearSynchronization();

            // Verify event was never sent
            verify(rabbitTemplate, never()).convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                    event
            );
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }
}
