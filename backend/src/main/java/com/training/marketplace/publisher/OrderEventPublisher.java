package com.training.marketplace.publisher;

import com.training.marketplace.config.RabbitMQConfig;
import com.training.marketplace.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCreatedEvent(OrderCreatedEvent event) {
        // After the GĐ2 transaction-boundary refactor (fe551b9) createOrder() is no longer wrapped in
        // a single @Transactional — the DB work runs in a TransactionTemplate and the event publish
        // happens outside any transaction. registerSynchronization would then throw
        // "Transaction synchronization is not active", breaking order creation. The event is consumed
        // idempotently by RabbitMQ consumers, so publishing directly is safe here.
        if (org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronization() {
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
        } else {
            log.info("Publishing OrderCreatedEvent (no active tx): eventId={}, orderId={}", event.eventId(), event.orderId());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                    event
            );
        }
    }
}
