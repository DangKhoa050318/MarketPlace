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
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.entity.RefundRequest;
import com.training.marketplace.entity.ReturnRequest;
import com.training.marketplace.entity.User;
import com.training.marketplace.dto.response.PaygatePayloadResponse;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.enums.RefundRequestStatus;
import com.training.marketplace.enums.Role;
import com.training.marketplace.event.OrderCreatedEvent;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.OrderMapper;
import com.training.marketplace.publisher.OrderEventPublisher;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.RefundRequestRepository;
import com.training.marketplace.repository.ReturnRequestRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.AppliedCoupon;
import com.training.marketplace.service.CartService;
import com.training.marketplace.service.DeliveryService;
import com.training.marketplace.service.InventoryFacade;
import com.training.marketplace.service.MerchandisingEventService;
import com.training.marketplace.service.OrderService;
import com.training.marketplace.service.PaymentService;
import com.training.marketplace.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.training.marketplace.repository.ProductVariantRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("3750000");
    private static final BigDecimal STANDARD_SHIPPING_FEE = new BigDecimal("125000");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;
    private final CartService cartService;
    private final InventoryFacade inventoryFacade;
    private final OrderMapper orderMapper;
    private final OrderEventPublisher orderEventPublisher;
    private final PromotionService promotionService;
    private final PaymentService paymentService;
    private final MerchandisingEventService merchandisingEventService;
    private final DeliveryService deliveryService;
    private final RefundRequestRepository refundRequestRepository;
    private final OrderExpiryJob orderExpiryJob;
    private final ReturnRequestRepository returnRequestRepository;
    private final TransactionTemplate transactionTemplate;

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

        // 2. Re-fetch authoritative prices from the DB and reject the checkout if any variant's current
        //    price differs from the cart (Redis) snapshot — the customer must review the new price before
        //    confirming instead of being charged a silently-changed amount.
        Map<Long, ProductVariant> variantById = new HashMap<>();
        for (ProductVariant v : variantRepository.findAllById(quantityByVariant.keySet())) {
            variantById.put(v.getId(), v);
        }
        List<String> priceChanges = new ArrayList<>();
        for (CartItemResponse item : cart.items()) {
            ProductVariant variant = variantById.get(item.variantId());
            if (variant == null) {
                throw new BadRequestException(
                        "Sản phẩm '" + item.productName() + "' không còn khả dụng, vui lòng xem lại giỏ hàng.");
            }
            BigDecimal currentPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
            if (item.unitPrice() == null || currentPrice.compareTo(item.unitPrice()) != 0) {
                priceChanges.add(String.format("%s (%s → %s)", item.productName(), item.unitPrice(), currentPrice));
            }
        }
        if (!priceChanges.isEmpty()) {
            throw new BadRequestException(
                    "Giá đã thay đổi, vui lòng xem lại giỏ hàng: " + String.join(", ", priceChanges));
        }

        // 3. Reserve stock (locks stock_levels rows, validates no oversell). Authoritative point.
        inventoryFacade.reserve(warehouseId, quantityByVariant);

        // 4. Build order + items using the (now-validated) current DB prices.
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
            ProductVariant variant = variantById.get(item.variantId());
            BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.quantity()));
            calculatedTotal = calculatedTotal.add(subtotal);
            order.addItem(OrderItem.builder()
                    .variantId(item.variantId())
                    .productId(item.productId())
                    .sku(item.sku())
                    .productName(item.productName())
                    .variantName(item.variantName())
                    .unitPrice(unitPrice)
                    .quantity(item.quantity())
                    .subtotal(subtotal)
                    .build());
        }

        // 3a. Charge 125,000 VND below the 3,750,000 VND free-shipping threshold.
        BigDecimal shippingFee = calculatedTotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0
                ? BigDecimal.ZERO
                : STANDARD_SHIPPING_FEE;
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
        BigDecimal upfront;
        BigDecimal finance;
        if (paymentMethod == PaymentMethod.PAYGATE_BNPL) {
            upfront = request.upfrontAmount() != null ? request.upfrontAmount() : grandTotal.multiply(new BigDecimal("0.30")).setScale(2, java.math.RoundingMode.HALF_UP);
            finance = request.financeAmount() != null ? request.financeAmount() : grandTotal.subtract(upfront);
            if (upfront.add(finance).compareTo(grandTotal) != 0) {
                throw new BadRequestException("Upfront amount and finance amount must exactly equal the total order amount");
            }
        } else {
            upfront = grandTotal;
            finance = BigDecimal.ZERO;
        }

        if (paymentMethod == PaymentMethod.WALLET) {
            BigDecimal walletBalance = user.getWalletBalance() != null ? user.getWalletBalance() : BigDecimal.ZERO;
            if (walletBalance.compareTo(grandTotal) < 0) {
                throw new BadRequestException("Marketplace wallet balance is insufficient");
            }
            user.setWalletBalance(walletBalance.subtract(grandTotal).setScale(2, java.math.RoundingMode.HALF_UP));
            userRepository.save(user);
        }

        order.setPaymentMethod(paymentMethod);
        order.setUpfrontAmount(upfront != null ? upfront : grandTotal);
        order.setFinanceAmount(finance != null ? finance : BigDecimal.ZERO);

        boolean zeroTotal = grandTotal.compareTo(BigDecimal.ZERO) == 0;
        if (zeroTotal || paymentMethod == PaymentMethod.WALLET) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.PAID);
        } else if (paymentMethod == PaymentMethod.COD) {
            order.setStatus(OrderStatus.CONFIRMED);
            order.setPaymentStatus(PaymentStatus.UNPAID);
        } else {
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentStatus(PaymentStatus.PENDING_PAYGATE);
        }

        Order savedOrder = orderRepository.save(order);

        if ((zeroTotal || paymentMethod == PaymentMethod.WALLET) && warehouseId != null && !quantityByVariant.isEmpty()) {
            inventoryFacade.fulfill(warehouseId, quantityByVariant);
        }

        if (appliedCoupon != null) {
            promotionService.recordRedemption(
                    appliedCoupon.promotionCodeId(), userId, savedOrder.getId(), discount);
        }

        try {
            merchandisingEventService.attributeOrder(savedOrder.getId(), userId, savedOrder.getCreatedAt());
        } catch (Exception ex) {
            log.warn("Merchandising attribution skipped for order {}: {}", savedOrder.getId(), ex.getMessage());
        }

        BigDecimal grandTotalFinal = savedOrder.getTotalAmount();
        PaymentMethod paymentMethodFinal = savedOrder.getPaymentMethod();

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
                savedOrder.getId(), userId, paymentMethodFinal, savedOrder.getStatus(), grandTotalFinal);

        OrderResponse baseResponse = orderMapper.toResponse(savedOrder);
        PaygatePayloadResponse paygatePayload = paymentService.createPaymentSession(savedOrder, paymentMethodFinal);

        // 4. Clear cart only after successful order placement and external API calls
        cartService.clearCart(userId);

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
                baseResponse.paygateTransactionRef(),
                savedOrder.getPaygateToken(),
                savedOrder.getPaygateUrl(),
                paygatePayload,
                savedOrder.getPaygateExpiresAt(),
                baseResponse.refundRequestId(),
                baseResponse.refundRequestStatus(),
                baseResponse.returnRequestId(),
                baseResponse.returnRequestStatus(),
                baseResponse.note(),
                baseResponse.items(),
                baseResponse.createdAt(),
                baseResponse.updatedAt()
        );
    }




    @Override
    @Transactional
    public PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return getUserOrders(userId, null, null, null, pageable);
    }

    @Override
    @Transactional
    public PageResponse<OrderResponse> getUserOrders(Long userId, OrderStatus status, com.training.marketplace.enums.PaymentStatus paymentStatus, String search, Pageable pageable) {
        String cleanSearch = (search != null && !search.trim().isEmpty()) ? "%" + search.trim().toLowerCase() + "%" : null;
        Page<Order> page = orderRepository.findFilteredOrders(userId, status, paymentStatus, cleanSearch, pageable);
        page.getContent().forEach(this::ensureOrderItemProductIds);
        return PageResponse.from(page, this::toOrderResponse);
    }

    @Override
    @Transactional
    public OrderResponse getUserOrderById(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to view this order");
        }
        ensureOrderItemProductIds(order);
        return enrichOrderResponse(order);
    }

    @Override
    public OrderResponse enrichOrderResponse(Order order) {
        if (order.getStatus() == OrderStatus.PENDING
                && order.getPaygateExpiresAt() != null
                && LocalDateTime.now().isAfter(order.getPaygateExpiresAt())) {
            orderExpiryJob.cancelExpiredOrder(order);
        }

        OrderResponse resp = orderMapper.toResponse(order);

        String effectivePaygateUrl = order.getPaygateUrl();
        if ((effectivePaygateUrl == null || effectivePaygateUrl.isBlank()) && order.getPaygateToken() != null) {
            effectivePaygateUrl = "http://localhost:4201/checkout?token=" + order.getPaygateToken();
        }
        if ((effectivePaygateUrl == null || effectivePaygateUrl.isBlank()) && requiresExternalPayment(order.getPaymentMethod())) {
            effectivePaygateUrl = "http://localhost:4201/checkout?orderId=ORD-" + order.getId();
        }

        PaygatePayloadResponse payload = resp.paygatePayload();
        if (payload == null && order.getStatus() == OrderStatus.PENDING && order.getPaymentStatus() != PaymentStatus.PAID
                && requiresExternalPayment(order.getPaymentMethod())) {
            String channel = order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "BANK_TRANSFER";
            payload = new PaygatePayloadResponse(
                    order.getId(),
                    order.getUser().getId(),
                    order.getTotalAmount(),
                    order.getUpfrontAmount(),
                    order.getFinanceAmount(),
                    channel,
                    effectivePaygateUrl,
                    null,
                    new com.training.marketplace.dto.response.PaygateCreateCheckoutResponse.BankAccountData("MBBank - Ngân hàng TMCP Quân Đội", "SYS0000000000000001", "PAYGATE GATEWAY SYSTEM", order.getTotalAmount()),
                    "PAYGATE ORD-" + order.getId(),
                    null
            );
        }

        return new OrderResponse(
                resp.id(), resp.userId(), resp.username(), resp.userEmail(), resp.shippingAddress(),
                resp.totalAmount(), resp.discountAmount(), resp.shippingFee(), resp.couponCode(),
                resp.status(), resp.paymentMethod(), resp.paymentStatus(), resp.upfrontAmount(),
                resp.financeAmount(), resp.paygateTransactionRef(), order.getPaygateToken(), effectivePaygateUrl,
                payload, order.getPaygateExpiresAt(),
                resp.refundRequestId(), resp.refundRequestStatus(), resp.returnRequestId(), resp.returnRequestStatus(),
                resp.note(), resp.items(), resp.createdAt(), resp.updatedAt()
        );
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

    private boolean requiresExternalPayment(PaymentMethod paymentMethod) {
        return paymentMethod != null && paymentMethod != PaymentMethod.COD && paymentMethod != PaymentMethod.WALLET;
    }

    @Override
    @Transactional
    public OrderResponse cancelUserOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to cancel this order");
        }

        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new BadRequestException("Order is already being delivered. Please create a return request instead.");
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BadRequestException("Delivered orders cannot be cancelled directly. Please contact support for returns.");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return toOrderResponse(order);
        }

        paymentService.cancelPayment(order, "Customer cancelled before shipment");

        order.setStatus(OrderStatus.CANCELLED);

        // Release the reservation held while the order was PENDING/CONFIRMED.
        if (order.getWarehouseId() != null) {
            inventoryFacade.release(order.getWarehouseId(), quantitiesByVariant(order));
        }

        // Refund the coupon redemption held for this order (frees a usage slot; idempotent).
        promotionService.refundIfPresent(order.getPromotionCodeId(), order.getId());

        Order savedOrder = orderRepository.save(order);
        log.info("User cancelled order {}: paymentStatus={}, reservation released", orderId, savedOrder.getPaymentStatus());
        return toOrderResponse(savedOrder);
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
        if (order.getPaymentMethod() == PaymentMethod.COD) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }
        Order saved = orderRepository.save(order);
        log.info("Customer confirmed receipt of order {}", orderId);
        return toOrderResponse(saved);
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
        return PageResponse.from(page, this::toOrderResponse);
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

        // When the order ships, convert the reservation into an actual stock decrement — but only if it
        // wasn't already fulfilled at payment time. Prepaid (PayGate webhook) and 0đ orders are fulfilled
        // when they reach PAID, so re-fulfilling here would decrement on-hand twice (phantom stock).
        if (request.status() == OrderStatus.SHIPPED
                && order.getWarehouseId() != null
                && order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0) {
            inventoryFacade.fulfill(order.getWarehouseId(), quantitiesByVariant(order));
        }

        // When an admin cancels, restore stock and refund any coupon (mirrors cancelUserOrder).
        // Guard against a CANCELLED -> CANCELLED no-op double release.
        if (request.status() == OrderStatus.CANCELLED && previousStatus != OrderStatus.CANCELLED) {
            paymentService.cancelPayment(order, "Admin cancelled before shipment");
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
        if (request.status() == OrderStatus.DELIVERED) {
            if (order.getDeliveredAt() == null) {
                order.setDeliveredAt(java.time.LocalDateTime.now());
            }
            if (order.getPaymentMethod() == PaymentMethod.COD) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
        }
        if (request.note() != null && !request.note().isBlank()) {
            order.setNote(request.note());
        }
        return toOrderResponse(orderRepository.save(order));
    }

    private OrderResponse toOrderResponse(Order order) {
        OrderResponse base = orderMapper.toResponse(order);
        RefundRequest latestRefund = order.getId() != null
                ? refundRequestRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()).orElse(null)
                : null;
        ReturnRequest latestReturn = order.getId() != null
                ? returnRequestRepository.findFirstByOrderIdOrderByCreatedAtDesc(order.getId()).orElse(null)
                : null;
        return new OrderResponse(
                base.id(),
                base.userId(),
                base.username(),
                base.userEmail(),
                base.shippingAddress(),
                base.totalAmount(),
                base.discountAmount(),
                base.shippingFee(),
                base.couponCode(),
                base.status(),
                base.paymentMethod(),
                base.paymentStatus(),
                base.upfrontAmount(),
                base.financeAmount(),
                base.paygateTransactionRef(),
                base.paygateToken(),
                base.paygateUrl(),
                base.paygatePayload(),
                order.getPaygateExpiresAt(),
                latestRefund != null ? latestRefund.getId() : null,
                latestRefund != null ? latestRefund.getStatus() : null,
                latestReturn != null ? latestReturn.getId() : null,
                latestReturn != null ? latestReturn.getStatus() : null,
                base.note(),
                base.items(),
                base.createdAt(),
                base.updatedAt()
        );
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


}
