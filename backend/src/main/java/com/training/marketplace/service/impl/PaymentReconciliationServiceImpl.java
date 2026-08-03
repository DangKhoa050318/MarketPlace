package com.training.marketplace.service.impl;

import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.PaymentAttempt;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentAttemptStatus;
import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.payment.PaymentResult;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.PaymentAttemptRepository;
import com.training.marketplace.service.InventoryFacade;
import com.training.marketplace.service.PaymentReconciliationService;
import com.training.marketplace.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentReconciliationServiceImpl implements PaymentReconciliationService {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final OrderRepository orderRepository;
    private final InventoryFacade inventoryFacade;
    private final PromotionService promotionService;

    @Override
    @Transactional
    public boolean prepare(OrderCreatedEvent event) {
        PaymentAttempt existing = paymentAttemptRepository.findByEventIdForUpdate(event.eventId()).orElse(null);
        if (existing != null) {
            if (!existing.getOrderId().equals(event.orderId())
                    || existing.getAmount().compareTo(event.totalAmount()) != 0) {
                throw new BadRequestException("Payment event id was reused with different order data");
            }
            return !isTerminal(existing.getStatus());
        }

        Order order = orderRepository.findByIdForUpdate(event.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", event.orderId()));
        PaymentAttemptStatus status = order.getStatus() == OrderStatus.PENDING
                ? PaymentAttemptStatus.PROCESSING
                : PaymentAttemptStatus.SKIPPED;
        paymentAttemptRepository.save(PaymentAttempt.builder()
                .eventId(event.eventId())
                .orderId(event.orderId())
                .amount(event.totalAmount())
                .status(status)
                .build());
        return status == PaymentAttemptStatus.PROCESSING;
    }

    @Override
    @Transactional
    public ReconciliationOutcome reconcile(OrderCreatedEvent event, PaymentResult result) {
        PaymentAttempt attempt = paymentAttemptRepository.findByEventIdForUpdate(event.eventId())
                .orElseThrow(() -> new BadRequestException("Payment attempt was not prepared"));
        if (isTerminal(attempt.getStatus())) {
            return ReconciliationOutcome.ALREADY_COMPLETED;
        }

        Order order = orderRepository.findByIdForUpdate(event.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", event.orderId()));

        if (result.approved()) {
            if (result.providerTransactionId() == null || result.providerTransactionId().isBlank()) {
                throw new BadRequestException("Approved payment is missing provider transaction id");
            }
            attempt.setProviderTransactionId(result.providerTransactionId());
            if (order.getStatus() == OrderStatus.CANCELLED) {
                attempt.setStatus(PaymentAttemptStatus.REFUND_REQUIRED);
                paymentAttemptRepository.save(attempt);
                return ReconciliationOutcome.REFUND_REQUIRED;
            }
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
                orderRepository.save(order);
            }
            attempt.setStatus(PaymentAttemptStatus.SUCCEEDED);
            paymentAttemptRepository.save(attempt);
            return ReconciliationOutcome.COMPLETED;
        }

        attempt.setFailureReason(truncate(result.failureReason(), 500));
        attempt.setStatus(PaymentAttemptStatus.DECLINED);
        if (order.getStatus() == OrderStatus.PENDING) {
            if (order.getWarehouseId() != null) {
                inventoryFacade.release(order.getWarehouseId(), quantitiesByVariant(order));
            }
            promotionService.refundIfPresent(order.getPromotionCodeId(), order.getId());
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
        }
        paymentAttemptRepository.save(attempt);
        return ReconciliationOutcome.COMPLETED;
    }

    @Override
    @Transactional
    public void markRefunded(String eventId) {
        PaymentAttempt attempt = paymentAttemptRepository.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new BadRequestException("Payment attempt not found"));
        if (attempt.getStatus() == PaymentAttemptStatus.REFUND_REQUIRED) {
            attempt.setStatus(PaymentAttemptStatus.REFUNDED);
            paymentAttemptRepository.save(attempt);
        }
    }

    private static boolean isTerminal(PaymentAttemptStatus status) {
        return status == PaymentAttemptStatus.SUCCEEDED
                || status == PaymentAttemptStatus.DECLINED
                || status == PaymentAttemptStatus.REFUNDED
                || status == PaymentAttemptStatus.SKIPPED;
    }

    private static String truncate(String value, int maxLength) {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static Map<Long, Integer> quantitiesByVariant(Order order) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            quantities.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
        }
        return quantities;
    }
}
