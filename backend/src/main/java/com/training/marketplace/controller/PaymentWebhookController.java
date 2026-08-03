package com.training.marketplace.controller;

import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.repository.OrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhook", description = "Endpoints for receiving payment gateway callbacks and notifications")
public class PaymentWebhookController {

    private final OrderRepository orderRepository;

    @PostMapping("/paygate-webhook")
    @Operation(summary = "Receive Webhook IPN notification from PayGate microservice")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handlePaygateWebhook(@RequestBody PaygateWebhookRequest payload) {
        log.info("Received PayGate Webhook payload: event={}, transactionRef={}, orderId={}, status={}, amount={}",
                payload.event(), payload.transactionRef(), payload.orderId(), payload.status(), payload.amount());

        if (payload.orderId() == null && payload.transactionRef() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid payload: orderId and transactionRef are null"));
        }

        Long orderId = null;
        if (payload.orderId() != null) {
            String rawId = payload.orderId().startsWith("ORD-") ? payload.orderId().substring(4) : payload.orderId();
            try {
                orderId = Long.parseLong(rawId);
            } catch (NumberFormatException e) {
                log.warn("Failed to parse order ID from PayGate payload orderId: {}", payload.orderId());
            }
        }

        if (orderId == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Could not resolve order ID"));
        }

        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            log.warn("Order not found for Webhook orderId: {}", orderId);
            return ResponseEntity.status(404).body(ApiResponse.error("Order not found: " + orderId));
        }

        if ("PAYMENT_COMPLETED".equalsIgnoreCase(payload.event()) || "SUCCESS".equalsIgnoreCase(payload.status())) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);
            log.info("Successfully updated order #{} to CONFIRMED and PAID via PayGate Webhook", orderId);
        }

        return ResponseEntity.ok(ApiResponse.success("PayGate Webhook processed successfully", Map.of(
                "orderId", order.getId(),
                "status", order.getStatus().name(),
                "paymentStatus", order.getPaymentStatus().name()
        )));
    }
}
