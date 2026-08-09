package com.training.marketplace.service;

import com.training.marketplace.dto.request.PaygateWebhookRequest;

import java.util.Map;

public interface PaymentWebhookService {

    /**
     * Process incoming Webhook IPN notification from PayGate microservice.
     * Verifies optional request signature/apiKey, checks idempotency, updates order status,
     * and performs stock fulfillment via InventoryFacade.
     *
     * @param payload PaygateWebhookRequest payload
     * @param signature Header signature or authorization token (optional)
     * @return Map containing response metadata (orderId, status, paymentStatus)
     */
    Map<String, Object> processPaygateWebhook(PaygateWebhookRequest payload, String signature, String rawPayload);
}
