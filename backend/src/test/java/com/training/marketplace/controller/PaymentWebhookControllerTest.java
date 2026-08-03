package com.training.marketplace.controller;

import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.entity.Order;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PaymentWebhookControllerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private PaymentWebhookController webhookController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handlePaygateWebhook_Success() {
        Order order = new Order();
        order.setId(100L);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING_PAYGATE);

        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-100",
                BigDecimal.valueOf(150.0),
                "SUCCESS"
        );

        ResponseEntity<?> response = webhookController.handlePaygateWebhook(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertEquals(PaymentStatus.PAID, order.getPaymentStatus());

        verify(orderRepository).save(order);
    }

    @Test
    void handlePaygateWebhook_OrderNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_000000",
                1L,
                "ORD-999",
                BigDecimal.valueOf(100.0),
                "SUCCESS"
        );

        ResponseEntity<?> response = webhookController.handlePaygateWebhook(request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
}
