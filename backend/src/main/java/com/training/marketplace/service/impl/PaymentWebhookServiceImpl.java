package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.service.InventoryFacade;
import com.training.marketplace.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookServiceImpl implements PaymentWebhookService {

    private final OrderRepository orderRepository;
    private final InventoryFacade inventoryFacade;

    @Value("${marketplace.paygate.api-key:mock-merchant-api-key-123456}")
    private String merchantApiKey;

    @Override
    @Transactional
    public Map<String, Object> processPaygateWebhook(PaygateWebhookRequest payload, String signature) {
        log.info("Processing PayGate Webhook: event={}, transactionRef={}, orderId={}, status={}, amount={}, signature={}",
                payload.event(), payload.transactionRef(), payload.orderId(), payload.status(), payload.amount(), signature);

        if (payload.orderId() == null && payload.transactionRef() == null) {
            throw new BadRequestException("Invalid payload: orderId and transactionRef are null");
        }

        // 1. Signature / Authorization Verification (Security Check)
        if (signature != null && !signature.isBlank()) {
            if (!signature.equals(merchantApiKey) && !signature.contains(merchantApiKey)) {
                log.warn("Invalid PayGate Webhook signature provided: {}", signature);
                throw new BadRequestException("Invalid webhook signature / unauthorized request");
            }
        }

        // 2. Parse Order ID
        Long orderId = parseOrderId(payload.orderId());
        if (orderId == null) {
            throw new BadRequestException("Could not resolve valid order ID from string: " + payload.orderId());
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // 3. Idempotency Check: If order is already in a terminal state (CONFIRMED or CANCELLED), return early without duplicate stock operations
        if (order.getStatus() == OrderStatus.CONFIRMED || order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Webhook received for order #{} which is already in terminal state {}. Returning idempotent result.", orderId, order.getStatus());
            return Map.of(
                    "orderId", order.getId(),
                    "status", order.getStatus().name(),
                    "paymentStatus", order.getPaymentStatus().name(),
                    "idempotent", true
            );
        }

        // 4. Process Status & Perform Stock Fulfillment / Release via InventoryFacade
        boolean isSuccess = "PAYMENT_COMPLETED".equalsIgnoreCase(payload.event()) ||
                            "SUCCESS".equalsIgnoreCase(payload.status());

        if (isSuccess) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.PAID);
            orderRepository.save(order);

            // Authoritative stock fulfillment in InventoryFacade
            if (order.getWarehouseId() != null && order.getItems() != null && !order.getItems().isEmpty()) {
                inventoryFacade.fulfill(order.getWarehouseId(), quantitiesByVariant(order));
            }
            log.info("Successfully processed PayGate Webhook: Order #{} updated to CONFIRMED and PAID, stock fulfilled.", orderId);
        } else if ("FAILED".equalsIgnoreCase(payload.status()) ||
                   "CANCELLED".equalsIgnoreCase(payload.status()) ||
                   "PAYMENT_CANCELLED".equalsIgnoreCase(payload.event()) ||
                   "PAYMENT_FAILED".equalsIgnoreCase(payload.event())) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            orderRepository.save(order);

            // Release reserved stock if payment fails or user cancels
            if (order.getWarehouseId() != null && order.getItems() != null && !order.getItems().isEmpty()) {
                inventoryFacade.release(order.getWarehouseId(), quantitiesByVariant(order));
            }
            log.info("PayGate Webhook reported failure/cancellation: Order #{} updated to CANCELLED and UNPAID, stock released.", orderId);
        }

        return Map.of(
                "orderId", order.getId(),
                "status", order.getStatus().name(),
                "paymentStatus", order.getPaymentStatus().name(),
                "idempotent", false
        );
    }

    private Long parseOrderId(String rawOrderId) {
        if (rawOrderId == null) {
            return null;
        }
        String cleanId = rawOrderId.startsWith("ORD-") ? rawOrderId.substring(4) : rawOrderId;
        try {
            return Long.parseLong(cleanId);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse numeric order ID from string: {}", rawOrderId);
            return null;
        }
    }

    private Map<Long, Integer> quantitiesByVariant(Order order) {
        Map<Long, Integer> quantityByVariant = new LinkedHashMap<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                quantityByVariant.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
            }
        }
        return quantityByVariant;
    }
}
