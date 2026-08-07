package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.service.PaymentWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhook", description = "Endpoints for receiving payment gateway callbacks and notifications")
public class PaymentWebhookController {

    private final PaymentWebhookService paymentWebhookService;

    @Value("${marketplace.paygate.api-key:mock-merchant-api-key-123456}")
    private String merchantApiKey;

    @PostMapping("/paygate-webhook")
    @Operation(summary = "Receive Webhook IPN notification from PayGate microservice")
    public ApiResponse<Map<String, Object>> handlePaygateWebhook(
            @RequestHeader(value = "X-Paygate-Signature", required = false) String signature,
            @RequestBody PaygateWebhookRequest payload) {
        log.info("Received PayGate Webhook request on endpoint /paygate-webhook");
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(payload, signature);
        return ApiResponse.success("PayGate Webhook processed successfully", result);
    }

    @PostMapping("/paygate-callback")
    @Operation(summary = "Confirm PayGate redirect callback from Marketplace frontend")
    public ApiResponse<Map<String, Object>> handlePaygateFrontendCallback(
            @RequestBody PaygateWebhookRequest payload) {
        log.info("Received PayGate frontend callback confirmation");
        Map<String, Object> result = paymentWebhookService.processPaygateWebhook(payload, merchantApiKey);
        return ApiResponse.success("PayGate callback processed successfully", result);
    }
}
