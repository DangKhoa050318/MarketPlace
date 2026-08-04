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

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhook", description = "Endpoints for receiving payment gateway callbacks and notifications")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @PostMapping("/paygate-webhook")
    @Operation(summary = "Receive Webhook IPN notification from PayGate microservice")
    public ApiResponse<Map<String, Object>> handlePaygateWebhook(
            @RequestHeader(value = "X-Paygate-Signature", required = false) String signature,
            @RequestBody PaygateWebhookRequest payload) {
        log.info("Received PayGate Webhook request on endpoint /paygate-webhook");
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(payload, signature);
        return ApiResponse.success("PayGate Webhook processed successfully", result);
    }
}
