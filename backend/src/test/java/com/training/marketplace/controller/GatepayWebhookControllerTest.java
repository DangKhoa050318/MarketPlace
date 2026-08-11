package com.training.marketplace.controller;

import com.training.marketplace.service.PaymentWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GatepayWebhookControllerTest {

    @Mock
    private PaymentWebhookService paymentWebhookService;

    @InjectMocks
    private GatepayWebhookController webhookController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void standardPayGateSignatureTakesPrecedenceOverLegacyHeader() {
        when(paymentWebhookService.processPaygateWebhook(any(), any(), any()))
                .thenReturn(Map.of("orderId", 33L));

        webhookController.handleGatepayWebhook("standard-signature", "legacy-signature", "{}");

        verify(paymentWebhookService)
                .processPaygateWebhook(any(), eq("standard-signature"), eq("{}"));
    }
}
