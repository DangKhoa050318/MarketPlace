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
        log.info("Publishing OrderCreatedEvent: eventId={}, orderId={}", event.eventId(), event.orderId());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CREATED_ROUTING_KEY,
                event
        );
    }
}
