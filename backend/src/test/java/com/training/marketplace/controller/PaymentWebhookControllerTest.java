package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.service.PaymentWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentWebhookControllerTest {

    @Mock
    private PaymentWebhookService paymentWebhookService;

    @InjectMocks
    private PaymentWebhookController webhookController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void handlePaygateWebhook_Success() {
        PaygateWebhookRequest request = new PaygateWebhookRequest(
                "PAYMENT_COMPLETED",
                "TXN_998877",
                1L,
                "ORD-100",
                BigDecimal.valueOf(150.0),
                "SUCCESS"
        );

        when(paymentWebhookService.processPaygateWebhook(any(), any()))
                .thenReturn(Map.of("orderId", 100L, "status", "CONFIRMED", "paymentStatus", "PAID"));

        ApiResponse<Map<String, Object>> response = webhookController.handlePaygateWebhook("signature-123", request);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData().get("orderId")).isEqualTo(100L);
        assertThat(response.getData().get("status")).isEqualTo("CONFIRMED");

        verify(paymentWebhookService).processPaygateWebhook(eq(request), eq("signature-123"));
    }
}
