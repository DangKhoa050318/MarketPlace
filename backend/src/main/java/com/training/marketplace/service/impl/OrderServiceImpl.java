package com.training.marketplace.service.impl;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateOrderRequest;
import com.training.marketplace.dto.request.UpdateOrderStatusRequest;
import com.training.marketplace.dto.response.CartItemResponse;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.dto.response.DashboardStatsResponse;
import com.training.marketplace.dto.response.OrderResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.User;
import com.training.marketplace.dto.response.PaygatePayloadResponse;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.enums.Role;
import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.OrderMapper;
import com.training.marketplace.publisher.OrderEventPublisher;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.AppliedCoupon;
import com.training.marketplace.service.CartService;
import com.training.marketplace.service.DeliveryService;
import com.training.marketplace.service.InventoryFacade;
import com.training.marketplace.service.MerchandisingEventService;
import com.training.marketplace.service.OrderService;
import com.training.marketplace.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.training.marketplace.repository.ProductVariantRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final InventoryFacade inventoryFacade;
    private final OrderMapper orderMapper;
    private final OrderEventPublisher orderEventPublisher;
    private final PromotionService promotionService;
    private final com.training.marketplace.service.PaygateClientService paygateClientService;
    private final MerchandisingEventService merchandisingEventService;
    private final DeliveryService deliveryService;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        CartResponse cart = cartService.getCart(userId);
        if (cart == null || cart.items().isEmpty()) {
            throw new BadRequestException("Shopping cart is empty");
        }

        Long warehouseId = inventoryFacade.defaultWarehouseId();

        // 1. Aggregate requested quantities per variant.
        Map<Long, Integer> quantityByVariant = new LinkedHashMap<>();
        for (CartItemResponse item : cart.items()) {
            quantityByVariant.merge(item.variantId(), item.quantity(), Integer::sum);
        }

        // 2. Reserve stock (locks stock_levels rows, validates no oversell). Authoritative point.
        inventoryFacade.reserve(warehouseId, quantityByVariant);

        // 3. Build order + items from cart snapshots.
        BigDecimal calculatedTotal = BigDecimal.ZERO;
        Order order = Order.builder()
                .user(user)
                .warehouseId(warehouseId)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .shippingAddress(request.shippingAddress())
                .note(request.note())
                .build();

        for (CartItemResponse item : cart.items()) {
            BigDecimal subtotal = item.unitPrice().multiply(BigDecimal.valueOf(item.quantity()));
            calculatedTotal = calculatedTotal.add(subtotal);
            order.addItem(OrderItem.builder()
                    .variantId(item.variantId())
                    .productId(item.productId())
                    .sku(item.sku())
                    .productName(item.productName())
                    .variantName(item.variantName())
                    .unitPrice(item.unitPrice())
                    .quantity(item.quantity())
                    .subtotal(subtotal)
                    .build());
        }

        // 3a. Calculate shipping fee ($5.00 if item subtotal < $150.00, FREE if >= $150.00)
        BigDecimal shippingFee = calculatedTotal.compareTo(new BigDecimal("150.00")) >= 0
                ? BigDecimal.ZERO
                : new BigDecimal("5.00");
        order.setShippingFee(shippingFee);

        // 3b. Apply coupon (optional). Locks the coupon row, validates against the cart, and
        //     increments used_count inside this transaction (no oversell of usage_limit).
        BigDecimal discount = BigDecimal.ZERO;
        AppliedCoupon appliedCoupon = null;
        if (StringUtils.hasText(request.couponCode())) {
            appliedCoupon = promotionService.consume(request.couponCode(), userId, cart);
            discount = appliedCoupon.discountAmount();
            order.setPromotionCodeId(appliedCoupon.promotionCodeId());
            order.setCouponCode(appliedCoupon.code());
        }
        order.setDiscountAmount(discount);

        BigDecimal grandTotal = calculatedTotal.subtract(discount).add(shippingFee);
        if (grandTotal.compareTo(BigDecimal.ZERO) < 0) {
            grandTotal = BigDecimal.ZERO;
        }
        order.setTotalAmount(grandTotal);

        PaymentMethod paymentMethod = request.paymentMethod() != null ? request.paymentMethod() : PaymentMethod.COD;
        BigDecimal upfront = request.upfrontAmount();
        BigDecimal finance = request.financeAmount();

        if (paymentMethod == PaymentMethod.COD || paymentMethod == PaymentMethod.CREDIT_CARD) {
            upfront = grandTotal;
            finance = BigDecimal.ZERO;
        } else if (paymentMethod == PaymentMethod.PAYGATE_BNPL) {
            if (upfront == null || upfront.compareTo(BigDecimal.ZERO) <= 0) {
                upfront = grandTotal.multiply(new BigDecimal("0.30")).setScale(2, java.math.RoundingMode.HALF_UP);
            }
            finance = grandTotal.subtract(upfront);
        }

        order.setPaymentMethod(paymentMethod);
        order.setUpfrontAmount(upfront != null ? upfront : grandTotal);
        order.setFinanceAmount(finance != null ? finance : BigDecimal.ZERO);

        if (paymentMethod == PaymentMethod.COD) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.UNPAID);
        } else {
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentStatus(PaymentStatus.PENDING_PAYGATE);
        }

        Order savedOrder = orderRepository.save(order);

        // 3c. Record the redemption now the order id is known.
        if (appliedCoupon != null) {
            promotionService.recordRedemption(
                    appliedCoupon.promotionCodeId(), userId, savedOrder.getId(), discount);
        }

        // 3d. Best-effort last-click merchandising attribution (B-408). Runs in its own transaction
        //     (REQUIRES_NEW) and must never break order creation, so failures are swallowed.
        try {
            merchandisingEventService.attributeOrder(savedOrder.getId(), userId, savedOrder.getCreatedAt());
        } catch (Exception ex) {
            log.warn("Merchandising attribution skipped for order {}: {}", savedOrder.getId(), ex.getMessage());
        }

        // 4. Clear cart.
        cartService.clearCart(userId);

        // 5. Publish OrderCreatedEvent (payment → notification; and downstream export bridge).
        List<OrderCreatedEvent.OrderItemInfo> eventItems = savedOrder.getItems().stream()
                .map(i -> new OrderCreatedEvent.OrderItemInfo(
                        i.getVariantId(), i.getSku(), i.getProductName(),
                        i.getUnitPrice(), i.getQuantity(), i.getSubtotal()))
                .toList();

        orderEventPublisher.publishOrderCreatedEvent(new OrderCreatedEvent(
                UUID.randomUUID().toString(),
                savedOrder.getId(),
                userId,
                user.getEmail(),
                warehouseId,
                savedOrder.getTotalAmount(),
                LocalDateTime.now(),
                eventItems));

        log.info("Order created: orderId={}, userId={}, paymentMethod={}, status={}, total={}",
                savedOrder.getId(), userId, paymentMethod, savedOrder.getStatus(), grandTotal);

        OrderResponse baseResponse = orderMapper.toResponse(savedOrder);
        PaygatePayloadResponse paygatePayload = null;
        if (paymentMethod != PaymentMethod.COD) {
            String methodStr = paymentMethod == PaymentMethod.BANK_TRANSFER ? "BANK_TRANSFER" : "WALLET";
            var pgSession = paygateClientService.createCheckoutSession(
                    savedOrder.getId(),
                    savedOrder.getTotalAmount(),
                    "Thanh toan don hang #" + savedOrder.getId() + " tren Marketplace",
                    methodStr
            );
            var sessionData = (pgSession != null) ? pgSession.data() : null;
            String targetPaymentUrl = (sessionData != null) ? sessionData.paymentUrl() : null;

            if (sessionData != null) {
                savedOrder.setPaygateToken(sessionData.token());
                savedOrder.setPaygateUrl(sessionData.paymentUrl());
                savedOrder.setPaygateExpiresAt(parseExpiresAt(sessionData.expiresAt()));
                orderRepository.save(savedOrder);
            }

            paygatePayload = new PaygatePayloadResponse(
                    savedOrder.getId(),
                    userId,
                    "mock-merchant-api-key-123456",
                    savedOrder.getTotalAmount(),
                    savedOrder.getUpfrontAmount(),
                    savedOrder.getFinanceAmount(),
                    paymentMethod.name(),
                    targetPaymentUrl,
                    sessionData != null ? sessionData.bankAccount() : null,
                    sessionData != null ? sessionData.transferContent() : null,
                    sessionData != null ? sessionData.qrPayload() : null
            );
        }

        return new OrderResponse(
                baseResponse.id(),
                baseResponse.userId(),
                baseResponse.username(),
                baseResponse.userEmail(),
                baseResponse.shippingAddress(),
                baseResponse.totalAmount(),
                baseResponse.discountAmount(),
                baseResponse.shippingFee(),
                baseResponse.couponCode(),
                baseResponse.status(),
                baseResponse.paymentMethod(),
                baseResponse.paymentStatus(),
                baseResponse.upfrontAmount(),
                baseResponse.financeAmount(),
                paygatePayload,
                baseResponse.note(),
                baseResponse.items(),
                baseResponse.createdAt(),
                baseResponse.updatedAt()
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
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            throw new BadRequestException("COD orders do not require online payment");
        }

        PaymentMethod paymentMethod = order.getPaymentMethod() != null ? order.getPaymentMethod() : PaymentMethod.CREDIT_CARD;

        // Check if an active PayGate session exists on the order and is still valid (< 15 minutes)
        if (order.getPaygateExpiresAt() != null && LocalDateTime.now().isBefore(order.getPaygateExpiresAt()) && order.getPaygateUrl() != null) {
            log.info("Reusing active PayGate session for order {}: expiresAt={}", order.getId(), order.getPaygateExpiresAt());
            return new PaygatePayloadResponse(
                    order.getId(),
                    userId,
                    "mock-merchant-api-key-123456",
                    order.getTotalAmount(),
                    order.getUpfrontAmount(),
                    order.getFinanceAmount(),
                    paymentMethod.name(),
                    order.getPaygateUrl(),
                    null, null, null
            );
        }

        String methodStr = paymentMethod == PaymentMethod.BANK_TRANSFER ? "BANK_TRANSFER" : "WALLET";

        var pgSession = paygateClientService.createCheckoutSession(
                order.getId(),
                order.getTotalAmount(),
                "Thanh toan lai don hang #" + order.getId() + " tren Marketplace",
                methodStr
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
                "mock-merchant-api-key-123456",
                order.getTotalAmount(),
                order.getUpfrontAmount(),
                order.getFinanceAmount(),
                paymentMethod.name(),
                targetPaymentUrl,
                sessionData != null ? sessionData.bankAccount() : null,
                sessionData != null ? sessionData.transferContent() : null,
                sessionData != null ? sessionData.qrPayload() : null
        );
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return getUserOrders(userId, null, null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getUserOrders(Long userId, OrderStatus status, com.training.marketplace.enums.PaymentStatus paymentStatus, String search, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? "%" + search.trim().toLowerCase() + "%" : null;
        Page<Order> page = orderRepository.findFilteredOrders(userId, status, paymentStatus, cleanSearch, pageable);
        page.getContent().forEach(this::ensureOrderItemProductIds);
        return PageResponse.from(page, orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getUserOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to view this order");
        }
        ensureOrderItemProductIds(order);
        return orderMapper.toResponse(order);
    }

    private void ensureOrderItemProductIds(Order order) {
        if (order.getItems() != null) {
            for (com.training.marketplace.entity.OrderItem item : order.getItems()) {
                if (item.getProductId() == null && item.getVariantId() != null) {
                    variantRepository.findById(item.getVariantId())
                            .ifPresent(v -> item.setProductId(v.getProductId()));
                }
            }
        }
    }

    @Override
    @Transactional
    public OrderResponse cancelUserOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to cancel this order");
        }
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Cannot cancel order in status " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Release the reservation held while the order was PENDING/CONFIRMED.
        if (order.getWarehouseId() != null) {
            inventoryFacade.release(order.getWarehouseId(), quantitiesByVariant(order));
        }

        // Refund the coupon redemption held for this order (frees a usage slot; idempotent).
        promotionService.refundIfPresent(order.getPromotionCodeId(), order.getId());

        Order savedOrder = orderRepository.save(order);
        log.info("User cancelled order {}: reservation released", orderId);
        return orderMapper.toResponse(savedOrder);
    }

    @Override
    @Transactional
    public OrderResponse confirmReceived(Long userId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to update this order");
        }
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException("Only a shipped order can be confirmed as received");
        }
        // Keep the delivery-tracking record in sync: close the delivery to DELIVERED (or reject if it
        // hasn't been picked up yet). No-op when the order has no delivery record.
        deliveryService.completeForCustomerConfirmation(orderId, userId);
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        log.info("Customer confirmed receipt of order {}", orderId);
        return orderMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getDashboardStats() {
        long totalOrders = orderRepository.count();
        BigDecimal totalRevenue = orderRepository.calculateTotalRevenue();
        long pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING);
        long completedOrders = orderRepository.countByStatus(OrderStatus.DELIVERED);
        long totalProducts = productRepository.countByActiveTrue();
        long totalCustomers = userRepository.countByRole(Role.CUSTOMER);
        return new DashboardStatsResponse(
                totalOrders, totalRevenue, pendingOrders, completedOrders, totalProducts, totalCustomers);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> getAdminOrders(OrderStatus status, Pageable pageable) {
        Page<Order> page = (status != null)
                ? orderRepository.findAllByStatus(status, pageable)
                : orderRepository.findAll(pageable);
        return PageResponse.from(page, orderMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countOrdersByUser(Long userId) {
        return orderRepository.countByUserId(userId);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatusByAdmin(Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        OrderStatus previousStatus = order.getStatus();
        validateStatusTransition(previousStatus, request.status());

        if (previousStatus == OrderStatus.SHIPPED && request.status() == OrderStatus.DELIVERED) {
            throw new BadRequestException(
                    "Complete the delivery tracking record to mark a shipped order as delivered");
        }

        // When the order ships, convert the reservation into an actual stock decrement.
        if (request.status() == OrderStatus.SHIPPED && order.getWarehouseId() != null) {
            inventoryFacade.fulfill(order.getWarehouseId(), quantitiesByVariant(order));
        }

        // When an admin cancels, restore stock and refund any coupon (mirrors cancelUserOrder).
        // Guard against a CANCELLED -> CANCELLED no-op double release.
        if (request.status() == OrderStatus.CANCELLED && previousStatus != OrderStatus.CANCELLED) {
            if (order.getWarehouseId() != null) {
                if (previousStatus == OrderStatus.SHIPPED) {
                    // Failed / refused delivery ("bom hàng"): the stock was already decremented at ship,
                    // so add it back on-hand instead of releasing a reservation that no longer exists.
                    inventoryFacade.returnStock(order.getWarehouseId(), quantitiesByVariant(order));
                } else {
                    inventoryFacade.release(order.getWarehouseId(), quantitiesByVariant(order));
                }
            }
            promotionService.refundIfPresent(order.getPromotionCodeId(), order.getId());
        }

        log.info("Admin updating order {} status {} -> {}", orderId, previousStatus, request.status());
        order.setStatus(request.status());
        if (request.status() == OrderStatus.DELIVERED && order.getDeliveredAt() == null) {
            order.setDeliveredAt(java.time.LocalDateTime.now());
        }
        if (request.note() != null && !request.note().isBlank()) {
            order.setNote(request.note());
        }
        return orderMapper.toResponse(orderRepository.save(order));
    }

    /** Aggregate an order's line items into {@code variantId -> total quantity} for inventory calls. */
    private static Map<Long, Integer> quantitiesByVariant(Order order) {
        Map<Long, Integer> quantityByVariant = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            quantityByVariant.merge(item.getVariantId(), item.getQuantity(), Integer::sum);
        }
        return quantityByVariant;
    }

    public void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.CANCELLED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!isValid) {
            throw new BadRequestException(
                    String.format("Cannot transition order status from %s to %s", currentStatus, newStatus));
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
}
