package com.training.marketplace.publisher;

import com.training.marketplace.config.RabbitMQConfig;
import com.training.marketplace.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Transaction synchronization is not active when publishing OrderCreatedEvent");
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Publishing OrderCreatedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.ORDER_EXCHANGE,
                            RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                            event
                    );
                }
            }
        );
    }
}
