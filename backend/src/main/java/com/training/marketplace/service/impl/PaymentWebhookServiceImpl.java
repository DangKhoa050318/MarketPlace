package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.PaygateWebhookRequest;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ForbiddenException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.service.InventoryFacade;
import com.training.marketplace.service.PaymentWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

    /**
     * When true (default), a webhook that arrives without a valid signature is rejected. The endpoint is
     * {@code permitAll()}, so this shared-secret check is the only authentication guarding it. Set
     * {@code marketplace.paygate.webhook.require-signature=false} only if the PayGate build in your
     * environment does not yet echo the secret in the {@code X-Paygate-Signature} header.
     */
    @Value("${marketplace.paygate.webhook.require-signature:true}")
    private boolean requireSignature;

    @Override
    @Transactional
    public Map<String, Object> processPaygateWebhook(PaygateWebhookRequest payload, String signature) {
        log.info("Processing PayGate Webhook: event={}, transactionRef={}, orderId={}, status={}, amount={}, signature={}",
                payload.event(), payload.transactionRef(), payload.orderId(), payload.status(), payload.amount(), signature);

        if (payload.orderId() == null && payload.transactionRef() == null) {
            throw new BadRequestException("Invalid payload: orderId and transactionRef are null");
        }

        if (payload.transactionRef() != null && payload.transactionRef().startsWith("TXN-REFUND-")) {
            log.info("Ignoring PayGate refund webhook transactionRef={} because Marketplace already marks refunds during cancellation",
                    payload.transactionRef());
            return Map.of(
                    "transactionRef", payload.transactionRef(),
                    "ignored", true,
                    "reason", "refund-notification"
            );
        }

        // 1. Signature / Authorization Verification (Security Check).
        // PayGate authenticates each webhook by sending the shared merchant secret in X-Paygate-Signature.
        verifySignature(signature);

        // 2. Parse Order ID
        Long orderId = parseOrderId(payload.orderId());
        if (orderId == null) {
            throw new BadRequestException("Could not resolve valid order ID from string: " + payload.orderId());
        }

        // Lock the order row (SELECT ... FOR UPDATE) so concurrent duplicate webhooks are serialized:
        // the idempotency check below then reads-and-acts atomically, closing the race where two
        // simultaneous retries both pass the guard and release/fulfill the same stock twice.
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // 3. Idempotency guard: PAID means fulfill already ran; CANCELLED means release already ran.
        // A webhook replayed in either terminal state must be a no-op — PayGate can retry the same
        // event multiple times, and re-running fulfill/release would corrupt stock (phantom stock).
        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Webhook for order #{} already in terminal state (status={}, payment={}). Idempotent skip.",
                    orderId, order.getStatus(), order.getPaymentStatus());
            return Map.of(
                    "orderId", order.getId(),
                    "status", order.getStatus().name(),
                    "paymentStatus", order.getPaymentStatus().name(),
                    "idempotent", true
            );
        }

        // 4. Process Status. Stock is only fulfilled when the order moves to SHIPPED.
        boolean isSuccess = "PAYMENT_COMPLETED".equalsIgnoreCase(payload.event()) ||
                            "SUCCESS".equalsIgnoreCase(payload.status()) ||
                            "COMPLETED".equalsIgnoreCase(payload.status());

        if (isSuccess) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.PAID);
            order.setPaygateTransactionRef(payload.transactionRef());
            orderRepository.save(order);
            log.info("Successfully processed PayGate Webhook: Order #{} updated to CONFIRMED and PAID.", orderId);
        } else if ("FAILED".equalsIgnoreCase(payload.status())
                || "CANCELLED".equalsIgnoreCase(payload.status())
                || "PAYMENT_CANCELLED".equalsIgnoreCase(payload.event())
                || "PAYMENT_FAILED".equalsIgnoreCase(payload.event())) {
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.UNPAID);
            orderRepository.save(order);

            // Release reserved stock when payment fails OR the user cancels checkout. Cancellation
            // may arrive via the status field (FAILED/CANCELLED) or the event field
            // (PAYMENT_CANCELLED/PAYMENT_FAILED) — cover both so a cancel-checkout webhook is not missed.
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

    /**
     * Authenticates the webhook against the shared PayGate secret. When enforcement is enabled a
     * missing/blank signature is rejected; a present signature must match the secret exactly. The
     * comparison is constant-time so the secret cannot be recovered through response-timing analysis.
     */
    private void verifySignature(String signature) {
        if (signature == null || signature.isBlank()) {
            if (requireSignature) {
                log.warn("Rejected PayGate webhook: missing X-Paygate-Signature header");
                throw new ForbiddenException("Missing webhook signature / unauthorized request");
            }
            log.warn("PayGate webhook accepted without a signature "
                    + "(enforcement disabled via marketplace.paygate.webhook.require-signature=false)");
            return;
        }
        if (!constantTimeEquals(signature, merchantApiKey)) {
            log.warn("Rejected PayGate webhook: invalid X-Paygate-Signature");
            throw new ForbiddenException("Invalid webhook signature / unauthorized request");
        }
    }

    private boolean constantTimeEquals(String provided, String expected) {
        if (expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
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
