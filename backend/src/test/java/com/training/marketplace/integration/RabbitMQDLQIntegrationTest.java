package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.config.RabbitMQConfig;
import com.training.marketplace.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RabbitMQDLQIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    void testRabbitMQDLQ_FailedMessageIsRoutedToDLQ() {
        // Step 1: Create a test event
        String eventId = UUID.randomUUID().toString();
        OrderCreatedEvent event = new OrderCreatedEvent(
                eventId,
                99999L,
                1L,
                "user1@example.com",
                1L,
                new BigDecimal("99.99"),
                LocalDateTime.now(),
                Collections.emptyList()
        );

        // Step 2: Publish to DLX / verify routing to payment.process.dlq
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_DLX,
                RabbitMQConfig.PAYMENT_DLQ,
                event
        );

        // Step 3: Wait and verify message is received from payment.process.dlq
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            OrderCreatedEvent receivedDlqEvent = (OrderCreatedEvent) rabbitTemplate.receiveAndConvert(RabbitMQConfig.PAYMENT_DLQ);
            assertThat(receivedDlqEvent).isNotNull();
            assertThat(receivedDlqEvent.eventId()).isEqualTo(eventId);
            assertThat(receivedDlqEvent.orderId()).isEqualTo(99999L);
        });
    }
}
