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
            @RequestHeader(value = "X-Paygate-Signature", required = false) String signature,
            @RequestBody PaygateWebhookRequest payload) {
        log.info("Received PayGate webhook on /api/v1/webhooks/gatepay endpoint");
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(payload, signature);
        return ApiResponse.success("PayGate Webhook processed successfully", result);
    }
}
