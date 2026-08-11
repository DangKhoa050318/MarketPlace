package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.service.PaymentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Secondary webhook endpoint matching the URL configured in PayGate's merchant seed
 * ({@code /api/v1/webhooks/gatepay}). Delegates to the same service as
 * {@link PaymentWebhookController}.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhook", description = "Alternative webhook endpoint matching PayGate merchant configuration")
public class GatepayWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/gatepay")
    @Operation(summary = "Receive IPN webhook from PayGate (merchant-configured URL)")
    public ApiResponse<Map<String, Object>> handleGatepayWebhook(
            @RequestHeader(value = "X-PayGate-Signature", required = false) String paygateSignature,
            @RequestHeader(value = "X-Signature", required = false) String legacySignature,
            @RequestBody String rawPayload) {
        log.info("Received PayGate webhook on /api/v1/webhooks/gatepay endpoint");

        PaygateWebhookRequest payload;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            payload = mapper.readValue(rawPayload, PaygateWebhookRequest.class);
        } catch (Exception e) {
            log.error("Failed to parse PayGate Webhook payload", e);
            throw new com.training.marketplace.exception.BadRequestException("Invalid webhook payload format");
        }

        String signature = paygateSignature != null && !paygateSignature.isBlank()
                ? paygateSignature : legacySignature;
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(payload, signature, rawPayload);
        return ApiResponse.success("PayGate Webhook processed successfully", result);
    }
}
