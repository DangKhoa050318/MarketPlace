package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.AdminReturnDecisionRequest;
import com.training.marketplace.dto.request.CreateReturnRequest;
import com.training.marketplace.dto.request.PartialRefundRequest;
import com.training.marketplace.dto.request.ReturnQcRequest;
import com.training.marketplace.dto.response.ReturnRequestResponse;
import com.training.marketplace.entity.Delivery;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.RefundRequest;
import com.training.marketplace.entity.ReturnRequest;
import com.training.marketplace.enums.DeliveryStatus;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.enums.RefundRequestStatus;
import com.training.marketplace.enums.ReturnRequestStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.DeliveryRepository;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.RefundRequestRepository;
import com.training.marketplace.repository.ReturnRequestRepository;
import com.training.marketplace.service.InventoryFacade;
import com.training.marketplace.service.PaygateClientService;
import com.training.marketplace.service.ReturnRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnRequestServiceImpl implements ReturnRequestService {

    private final OrderRepository orderRepository;
    private final DeliveryRepository deliveryRepository;
    private final ReturnRequestRepository returnRequestRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final PaygateClientService paygateClientService;
    private final InventoryFacade inventoryFacade;

    @Override
    @Transactional
    public ReturnRequestResponse create(Long userId, Long orderId, CreateReturnRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to request a return for this order");
        }

        if (request == null || request.reason() == null || request.reason().isBlank()) {
            throw new BadRequestException("Return reason is required");
        }
        List<String> evidenceImageUrls = normalizeEvidenceImageUrls(request.evidenceImageUrls());

        boolean returnableStatus = order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED;
        if (!returnableStatus) {
            throw new BadRequestException("Return request is only available after the order has shipped");
        }
        deliveryRepository.findByOrderId(orderId).ifPresent(delivery -> {
            if (order.getStatus() == OrderStatus.SHIPPED
                    && delivery.getStatus() != DeliveryStatus.IN_TRANSIT
                    && delivery.getStatus() != DeliveryStatus.DELIVERED) {
                throw new BadRequestException("Return request requires an active delivery");
            }
        });

        OrderItem targetItem = null;
        Integer quantity = null;
        if (request.orderItemId() != null) {
            targetItem = findOrderItem(order, request.orderItemId());
            quantity = request.quantity();
            if (quantity == null || quantity <= 0) {
                throw new BadRequestException("Return quantity must be greater than zero");
            }
            int remaining = targetItem.getQuantity() - safeRefundedQuantity(targetItem);
            if (quantity > remaining) {
                throw new BadRequestException("Return quantity exceeds refundable item quantity");
            }
        }

        returnRequestRepository.findByOrderIdAndStatusIn(
                orderId, List.of(ReturnRequestStatus.REQUESTED, ReturnRequestStatus.APPROVED, ReturnRequestStatus.RETURN_RECEIVED))
                .ifPresent(existing -> {
                    throw new BadRequestException("A return request is already open for this order");
                });

        ReturnRequest saved = returnRequestRepository.save(ReturnRequest.builder()
                .orderId(orderId)
                .userId(userId)
                .orderItemId(targetItem != null ? targetItem.getId() : null)
                .quantity(quantity)
                .status(ReturnRequestStatus.REQUESTED)
                .reason(request.reason())
                .evidenceImageUrls(String.join("\n", evidenceImageUrls))
                .build());
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnRequestResponse> getOpenRequests() {
        return returnRequestRepository.findAllByStatusInOrderByCreatedAtDesc(List.of(
                        ReturnRequestStatus.REQUESTED,
                        ReturnRequestStatus.APPROVED,
                        ReturnRequestStatus.RETURN_RECEIVED,
                        ReturnRequestStatus.QC_PASSED,
                        ReturnRequestStatus.QC_FAILED))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReturnRequestResponse decide(Long returnRequestId, AdminReturnDecisionRequest request) {
        ReturnRequest rr = getReturnRequest(returnRequestId);
        if (rr.getStatus() != ReturnRequestStatus.REQUESTED) {
            throw new BadRequestException("Only requested returns can be approved or rejected");
        }
        rr.setAdminNote(request != null ? request.note() : null);
        if (request == null || !request.approved()) {
            rr.setStatus(ReturnRequestStatus.REJECTED);
            return toResponse(returnRequestRepository.save(rr));
        }

        if (request.refundWithoutReturn()) {
            rr.setRefundWithoutReturn(true);
            boolean refunded = refundForReturn(rr, "Refund without return approved by admin");
            rr.setStatus(refunded ? ReturnRequestStatus.COMPLETED : ReturnRequestStatus.APPROVED);
        } else {
            rr.setStatus(ReturnRequestStatus.APPROVED);
        }
        return toResponse(returnRequestRepository.save(rr));
    }

    @Override
    @Transactional
    public ReturnRequestResponse recordQc(Long returnRequestId, ReturnQcRequest request) {
        ReturnRequest rr = getReturnRequest(returnRequestId);
        if (rr.getStatus() != ReturnRequestStatus.APPROVED && rr.getStatus() != ReturnRequestStatus.RETURN_RECEIVED) {
            throw new BadRequestException("Only approved returns can be QC checked");
        }
        rr.setQcNote(request != null ? request.note() : null);
        if (request != null && request.passed()) {
            restockReturnedItems(rr);
            boolean refunded = refundForReturn(rr, "Return QC passed");
            rr.setStatus(refunded ? ReturnRequestStatus.COMPLETED : ReturnRequestStatus.QC_PASSED);
        } else {
            rr.setStatus(ReturnRequestStatus.QC_FAILED);
        }
        return toResponse(returnRequestRepository.save(rr));
    }

    @Override
    @Transactional
    public ReturnRequestResponse partialRefund(Long orderId, PartialRefundRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (request == null || request.orderItemId() == null || request.quantity() == null || request.quantity() <= 0) {
            throw new BadRequestException("Partial refund requires order item and positive quantity");
        }
        OrderItem item = findOrderItem(order, request.orderItemId());
        int remaining = item.getQuantity() - safeRefundedQuantity(item);
        if (request.quantity() > remaining) {
            throw new BadRequestException("Refund quantity exceeds refundable item quantity");
        }
        ReturnRequest rr = returnRequestRepository.save(ReturnRequest.builder()
                .orderId(orderId)
                .userId(order.getUser().getId())
                .orderItemId(item.getId())
                .quantity(request.quantity())
                .status(ReturnRequestStatus.COMPLETED)
                .reason(request.reason() != null && !request.reason().isBlank() ? request.reason() : "Admin partial refund")
                .adminNote("Admin partial refund without return request")
                .refundWithoutReturn(true)
                .build());
        refundItemQuantity(order, item, request.quantity(), rr.getReason(), "PAYGATE_REFUND:ORDER:" + orderId + ":ITEM:" + item.getId() + ":QTY:" + request.quantity());
        return toResponse(rr);
    }

    private boolean refundForReturn(ReturnRequest rr, String fallbackReason) {
        Order order = orderRepository.findByIdForUpdate(rr.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", rr.getOrderId()));
        String reason = rr.getReason() != null && !rr.getReason().isBlank() ? rr.getReason() : fallbackReason;
        boolean refunded;
        if (rr.getOrderItemId() != null) {
            OrderItem item = findOrderItem(order, rr.getOrderItemId());
            int quantity = rr.getQuantity() != null ? rr.getQuantity() : item.getQuantity() - safeRefundedQuantity(item);
            refunded = refundItemQuantity(order, item, quantity, reason, "PAYGATE_REFUND:RETURN:" + rr.getId());
        } else {
            refunded = refundWholeOrder(order, reason, "PAYGATE_REFUND:RETURN:" + rr.getId() + ":FULL");
        }
        orderRepository.save(order);
        return refunded;
    }

    private boolean refundWholeOrder(Order order, String reason, String idempotencyKey) {
        BigDecimal remaining = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            int refundable = item.getQuantity() - safeRefundedQuantity(item);
            if (refundable > 0) {
                remaining = remaining.add(item.getUnitPrice().multiply(BigDecimal.valueOf(refundable)));
            }
        }
        remaining = remaining.add(order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Order has no refundable amount remaining");
        }
        if (processPaygateRefund(order, null, null, remaining, reason, idempotencyKey)) {
            for (OrderItem item : order.getItems()) {
                item.setRefundedQuantity(item.getQuantity());
            }
            order.setPaymentStatus(PaymentStatus.REFUNDED);
            return true;
        }
        return false;
    }

    private boolean refundItemQuantity(Order order, OrderItem item, int quantity, String reason, String idempotencyKey) {
        if (order.getPaymentStatus() != PaymentStatus.PAID && order.getPaymentStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new BadRequestException("Only paid orders can be refunded");
        }
        BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
        if (processPaygateRefund(order, item.getId(), quantity, amount, reason, idempotencyKey)) {
            item.setRefundedQuantity(safeRefundedQuantity(item) + quantity);
            boolean allRefunded = order.getItems().stream().allMatch(i -> safeRefundedQuantity(i) >= i.getQuantity());
            order.setPaymentStatus(allRefunded ? PaymentStatus.REFUNDED : PaymentStatus.PARTIALLY_REFUNDED);
            return true;
        }
        return false;
    }

    private void restockReturnedItems(ReturnRequest rr) {
        Order order = orderRepository.findByIdForUpdate(rr.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", rr.getOrderId()));
        Map<Long, Integer> quantityByVariant = new LinkedHashMap<>();
        if (rr.getOrderItemId() != null) {
            OrderItem item = findOrderItem(order, rr.getOrderItemId());
            int quantity = rr.getQuantity() != null ? rr.getQuantity() : item.getQuantity() - safeRefundedQuantity(item);
            quantityByVariant.put(item.getVariantId(), quantity);
        } else {
            for (OrderItem item : order.getItems()) {
                int quantity = item.getQuantity() - safeRefundedQuantity(item);
                if (quantity > 0) {
                    quantityByVariant.merge(item.getVariantId(), quantity, Integer::sum);
                }
            }
        }
        if (!quantityByVariant.isEmpty()) {
            inventoryFacade.restockReturn(order.getWarehouseId(), quantityByVariant);
        }
    }

    private boolean processPaygateRefund(Order order, Long orderItemId, Integer quantity, BigDecimal amount, String reason, String idempotencyKey) {
        RefundRequest refundRequest = refundRequestRepository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> refundRequestRepository.save(RefundRequest.builder()
                        .order(order)
                        .idempotencyKey(idempotencyKey)
                        .transactionRef(order.getPaygateTransactionRef())
                        .orderItemId(orderItemId)
                        .quantity(quantity)
                        .amount(amount)
                        .reason(reason)
                        .status(RefundRequestStatus.PENDING)
                        .build()));
        if (refundRequest.getStatus() == RefundRequestStatus.SUCCEEDED) {
            return true;
        }
        if (order.getPaygateTransactionRef() == null || order.getPaygateTransactionRef().isBlank()) {
            refundRequest.setStatus(RefundRequestStatus.FAILED);
            refundRequest.setFailureReason("PayGate transaction reference is missing");
            refundRequestRepository.save(refundRequest);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            return false;
        }
        try {
            paygateClientService.refund(order.getPaygateTransactionRef(), order.getId(), amount, idempotencyKey);
            refundRequest.setStatus(RefundRequestStatus.SUCCEEDED);
            refundRequest.setFailureReason(null);
            refundRequestRepository.save(refundRequest);
            return true;
        } catch (Exception ex) {
            refundRequest.setStatus(RefundRequestStatus.FAILED);
            refundRequest.setFailureReason(ex.getMessage());
            refundRequestRepository.save(refundRequest);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            log.warn("Return refund failed for order {}: {}", order.getId(), ex.getMessage());
            return false;
        }
    }

    private ReturnRequest getReturnRequest(Long id) {
        return returnRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ReturnRequest", id));
    }

    private OrderItem findOrderItem(Order order, Long orderItemId) {
        return order.getItems().stream()
                .filter(item -> Objects.equals(item.getId(), orderItemId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Order item does not belong to this order"));
    }

    private int safeRefundedQuantity(OrderItem item) {
        return item.getRefundedQuantity() != null ? item.getRefundedQuantity() : 0;
    }

    private ReturnRequestResponse toResponse(ReturnRequest request) {
        return new ReturnRequestResponse(
                request.getId(),
                request.getOrderId(),
                request.getUserId(),
                request.getOrderItemId(),
                request.getQuantity(),
                request.getStatus(),
                request.getReason(),
                parseEvidenceImageUrls(request.getEvidenceImageUrls()),
                request.getAdminNote(),
                request.getQcNote(),
                request.isRefundWithoutReturn(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    private List<String> normalizeEvidenceImageUrls(List<String> urls) {
        if (urls == null) {
            return List.of();
        }
        List<String> normalized = urls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .distinct()
                .toList();
        if (normalized.size() > 5) {
            throw new BadRequestException("Return request can include at most 5 evidence images");
        }
        return normalized;
    }

    private List<String> parseEvidenceImageUrls(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return Stream.of(stored.split("\\R"))
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .toList();
    }
}
