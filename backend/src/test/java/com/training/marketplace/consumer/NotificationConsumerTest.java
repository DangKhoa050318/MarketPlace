package com.training.marketplace.consumer;

import com.training.marketplace.event.OrderCreatedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Test
    void sendOrderConfirmationEmail_validEvent_sendsEmailViaJavaMailSender() {
        // Given
        var item = new OrderCreatedEvent.OrderItemInfo(1L, "SKU-1", "Wireless Mouse", BigDecimal.valueOf(25.00), 2, BigDecimal.valueOf(50.00));
        var event = new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                102L,
                10L,
                "user10@example.com",
                1L,
                BigDecimal.valueOf(50.00),
                LocalDateTime.now(),
                List.of(item)
        );

        // When
        notificationConsumer.sendOrderConfirmationEmail(event);

        // Then
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage);
        assertEquals("user10@example.com", sentMessage.getTo()[0]);
        assertEquals("Order Confirmation - Order #102", sentMessage.getSubject());
    }
}
