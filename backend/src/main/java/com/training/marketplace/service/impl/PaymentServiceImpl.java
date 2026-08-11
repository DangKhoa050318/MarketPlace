package com.training.marketplace.service.impl;

import com.training.marketplace.dto.response.PaygateCreateCheckoutResponse;
import com.training.marketplace.dto.response.PaygatePayloadResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.RefundRequest;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.enums.RefundRequestStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.RefundRequestRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.InventoryFacade;
import com.training.marketplace.service.PaygateClientService;
import com.training.marketplace.service.PaymentService;
import com.training.marketplace.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final OrderRepository orderRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final InventoryFacade inventoryFacade;
    private final PromotionService promotionService;
    private final PaygateClientService paygateClientService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PaygatePayloadResponse createPaymentSession(Order order, PaymentMethod paymentMethod) {
        boolean zeroTotal = order.getTotalAmount().compareTo(java.math.BigDecimal.ZERO) == 0;
        if (paymentMethod == PaymentMethod.COD || paymentMethod == PaymentMethod.WALLET || zeroTotal) {
            return null;
        }

        String methodStr;
        if (paymentMethod == PaymentMethod.BANK_TRANSFER) {
            methodStr = "VIETQR";
        } else if (paymentMethod == PaymentMethod.PAYGATE_BNPL) {
            methodStr = "BNPL";
        } else {
            methodStr = "PAYGATE";
        }
        var pgSession = paygateClientService.createCheckoutSession(
                order.getId(),
                order.getTotalAmount(),
                "Thanh toan don hang #" + order.getId() + " tren Marketplace",
                methodStr,
                order.getUpfrontAmount(),
                order.getFinanceAmount(),
                order.getUser().getId(),
                order.getUser().getFullName()
        );
        var sessionData = (pgSession != null) ? pgSession.data() : null;
        String targetPaymentUrl = (sessionData != null) ? sessionData.paymentUrl() : null;

        if (sessionData != null) {
            order.setPaygateToken(sessionData.token());
            order.setPaygateUrl(sessionData.paymentUrl());
            order.setPaygateExpiresAt(parseExpiresAt(sessionData.expiresAt()));
            orderRepository.save(order);
        }

        return new PaygatePayloadResponse(
                order.getId(),
                order.getUser().getId(),
                order.getTotalAmount(),
                order.getUpfrontAmount(),
                order.getFinanceAmount(),
                paymentMethod.name(),
                targetPaymentUrl,
                sessionData != null ? sessionData.vietQrUrl() : null,
                sessionData != null && sessionData.accountNumber() != null ? new PaygateCreateCheckoutResponse.BankAccountData(
                        sessionData.bankCode(), sessionData.accountNumber(), sessionData.accountName(), order.getTotalAmount()
                ) : null,
                sessionData != null ? sessionData.transferContent() : null,
                sessionData != null ? sessionData.qrCodePayload() : null
        );
    }

    @Override
    @Transactional
    public PaygatePayloadResponse retryOrderPayment(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to pay for this order");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled order");
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }
        if (order.getPaymentMethod() == PaymentMethod.COD || order.getPaymentMethod() == PaymentMethod.WALLET) {
            throw new BadRequestException(order.getPaymentMethod() + " orders do not require online payment");
        }

        PaymentMethod paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod() : PaymentMethod.CREDIT_CARD;

        // Check if an active PayGate session exists on the order and is still valid (< 15 minutes)
        if (order.getPaygateExpiresAt() != null && LocalDateTime.now().isBefore(order.getPaygateExpiresAt()) && order.getPaygateUrl() != null) {
            log.info("Reusing active PayGate session for order {}: expiresAt={}", order.getId(), order.getPaygateExpiresAt());
            return new PaygatePayloadResponse(
                    order.getId(),
                    userId,
                    order.getTotalAmount(),
                    order.getUpfrontAmount(),
                    order.getFinanceAmount(),
                    paymentMethod.name(),
                    order.getPaygateUrl(),
                    null, null, null, null
            );
        }

        String methodStr;
        if (paymentMethod == PaymentMethod.BANK_TRANSFER) {
            methodStr = "VIETQR";
        } else if (paymentMethod == PaymentMethod.PAYGATE_BNPL) {
            methodStr = "BNPL";
        } else {
            methodStr = "PAYGATE";
        }

        var pgSession = paygateClientService.createCheckoutSession(
                order.getId(),
                order.getTotalAmount(),
                "Thanh toan lai don hang #" + order.getId() + " tren Marketplace",
                methodStr,
                order.getUpfrontAmount(),
                order.getFinanceAmount(),
                order.getUser().getId(),
                order.getUser().getFullName()
        );

        var sessionData = (pgSession != null) ? pgSession.data() : null;
        String targetPaymentUrl = (sessionData != null) ? sessionData.paymentUrl() : null;

        if (sessionData != null) {
            order.setPaygateToken(sessionData.token());
            order.setPaygateUrl(sessionData.paymentUrl());
            order.setPaygateExpiresAt(parseExpiresAt(sessionData.expiresAt()));
        }

        order.setPaymentStatus(PaymentStatus.PENDING_PAYGATE);
        orderRepository.save(order);

        return new PaygatePayloadResponse(
                order.getId(),
                userId,
                order.getTotalAmount(),
                order.getUpfrontAmount(),
                order.getFinanceAmount(),
                paymentMethod.name(),
                targetPaymentUrl,
                sessionData != null ? sessionData.vietQrUrl() : null,
                sessionData != null && sessionData.accountNumber() != null ? new PaygateCreateCheckoutResponse.BankAccountData(
                        sessionData.bankCode(), sessionData.accountNumber(), sessionData.accountName(), order.getTotalAmount()
                ) : null,
                sessionData != null ? sessionData.transferContent() : null,
                sessionData != null ? sessionData.qrCodePayload() : null
        );
    }

    @Override
    @Transactional
    public Order cancelVietQrPayment(Long userId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to cancel this order");
        }

        // Idempotency check: if order is already CANCELLED, return current state
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.info("Order #{} is already cancelled. Idempotent skip.", orderId);
            return order;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("Cannot cancel payment for order in status " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setPaymentStatus(PaymentStatus.UNPAID);

        // Release the reservation held while the order was PENDING.
        if (order.getWarehouseId() != null) {
            inventoryFacade.release(order.getWarehouseId(), quantitiesByVariant(order));
        }

        // Refund the coupon redemption held for this order (frees a usage slot; idempotent).
        promotionService.refundIfPresent(order.getPromotionCodeId(), order.getId());

        Order savedOrder = orderRepository.save(order);
        log.info("User cancelled VietQR payment for order {}: reservation released, coupon refunded", orderId);
        return savedOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public Order confirmVietQrPayment(Long userId, Long orderId) {
        // Use a regular read — NOT findByIdForUpdate — because the outbound HTTP call to PayGate
        // below will synchronously trigger a webhook callback that itself acquires FOR UPDATE on
        // this same row. Holding a pessimistic lock here while blocking on the HTTP round-trip
        // would deadlock (this tx waits for PayGate, PayGate's webhook waits for this tx's lock).
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to perform payment for this order");
        }

        // Idempotency check: if order is already PAID or CONFIRMED, return current state without double fulfillment
        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getStatus() == OrderStatus.CONFIRMED) {
            log.info("Order #{} is already confirmed and paid. Idempotent skip.", orderId);
            return order;
        }

        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Order is cancelled and cannot be paid");
        }

        // Trigger S2S Bank Transfer simulation to PayGate.
        // PayGate will process settlement and send a signed Webhook (X-PayGate-Signature) back to MarketPlace.
        // The webhook handler (PaymentWebhookServiceImpl) will acquire its own FOR UPDATE lock
        // and atomically update the order to CONFIRMED + PAID.
        paygateClientService.simulateBankTransfer(orderId, order.getTotalAmount());

        log.info("Triggered S2S VietQR transfer for Order #{}. Awaiting/processed PayGate signed Webhook.", orderId);
        return orderRepository.findById(orderId).orElse(order);
    }

    @Override
    @Transactional
    public void cancelPayment(Order order, String reason) {
        boolean paidOnline = order.getPaymentStatus() == PaymentStatus.PAID
                && order.getPaymentMethod() != PaymentMethod.COD;

        if (paidOnline) {
            if (order.getPaymentMethod() == PaymentMethod.PAYGATE_BNPL) {
                processBnplMarketplaceWalletCredit(order, reason);
            } else {
                processRefund(order, reason);
            }
        } else if (order.getPaymentStatus() == PaymentStatus.PENDING_PAYGATE) {
            order.setPaymentStatus(PaymentStatus.UNPAID);
        }
    }

    private void processBnplMarketplaceWalletCredit(Order order, String reason) {
        String idempotencyKey = "PAYGATE_BNPL_CREDIT:ORDER:" + order.getId() + ":FULL";
        RefundRequest refundRequest = refundRequestRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> refundRequestRepository.save(RefundRequest.builder()
                        .order(order)
                        .idempotencyKey(idempotencyKey)
                        .transactionRef(order.getPaygateTransactionRef())
                        .amount(order.getTotalAmount())
                        .reason(reason)
                        .status(RefundRequestStatus.PENDING)
                        .build()));

        if (refundRequest.getStatus() == RefundRequestStatus.SUCCEEDED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            return;
        }

        var user = order.getUser();
        var currentBalance = user.getWalletBalance() != null ? user.getWalletBalance() : java.math.BigDecimal.ZERO;
        user.setWalletBalance(currentBalance.add(order.getTotalAmount()).setScale(2, java.math.RoundingMode.HALF_UP));
        userRepository.save(user);

        refundRequest.setTransactionRef(order.getPaygateTransactionRef());
        refundRequest.setStatus(RefundRequestStatus.SUCCEEDED);
        refundRequest.setFailureReason(null);
        refundRequestRepository.save(refundRequest);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        log.info("Credited Marketplace wallet for user {} by {} from cancelled BNPL order {}. Reason: {}",
                user.getId(), order.getTotalAmount(), order.getId(), reason);
    }

    private void processRefund(Order order, String reason) {
        String idempotencyKey = "PAYGATE_REFUND:ORDER:" + order.getId() + ":FULL";
        RefundRequest refundRequest = refundRequestRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> refundRequestRepository.save(RefundRequest.builder()
                        .order(order)
                        .idempotencyKey(idempotencyKey)
                        .transactionRef(order.getPaygateTransactionRef())
                        .amount(order.getTotalAmount())
                        .reason(reason)
                        .status(RefundRequestStatus.PENDING)
                        .build()));

        if (refundRequest.getStatus() == RefundRequestStatus.SUCCEEDED) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            return;
        }

        if (order.getPaygateTransactionRef() == null || order.getPaygateTransactionRef().isBlank()) {
            refundRequest.setStatus(RefundRequestStatus.FAILED);
            refundRequest.setFailureReason("PayGate transaction reference is missing");
            refundRequestRepository.save(refundRequest);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            log.warn("Order {} refund pending because PayGate transaction reference is missing", order.getId());
            return;
        }
        try {
            paygateClientService.refund(
                    order.getPaygateTransactionRef(),
                    order.getId(),
                    order.getTotalAmount(),
                    idempotencyKey);
            refundRequest.setTransactionRef(order.getPaygateTransactionRef());
            refundRequest.setStatus(RefundRequestStatus.SUCCEEDED);
            refundRequest.setFailureReason(null);
            refundRequestRepository.save(refundRequest);
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        } catch (Exception ex) {
            refundRequest.setTransactionRef(order.getPaygateTransactionRef());
            refundRequest.setStatus(RefundRequestStatus.FAILED);
            refundRequest.setFailureReason(ex.getMessage());
            refundRequestRepository.save(refundRequest);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            log.warn("Order {} refund pending because PayGate refund call failed: {}", order.getId(), ex.getMessage());
        }
    }

    private LocalDateTime parseExpiresAt(String expiresAtStr) {
        if (expiresAtStr == null || expiresAtStr.isBlank()) {
            return LocalDateTime.now().plusMinutes(15);
        }
        try {
            return LocalDateTime.parse(expiresAtStr);
        } catch (Exception e) {
            return LocalDateTime.now().plusMinutes(15);
        }
    }

    /** Aggregate an order's line items into {@code variantId -> total quantity} for inventory calls. */
    private static Map<Long, Integer> quantitiesByVariant(Order order) {
        Map<Long, Integer> quantityByVariant = new LinkedHashMap<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                quantityByVariant.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
            }
        }
        return quantityByVariant;
    }
}
