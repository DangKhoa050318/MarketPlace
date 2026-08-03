package com.training.marketplace.dto.response;

import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        String username,
        String userEmail,
        String shippingAddress,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        String couponCode,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal upfrontAmount,
        BigDecimal financeAmount,
        PaygatePayloadResponse paygatePayload,
        String note,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public OrderResponse(
            Long id, Long userId, String username, String userEmail, String shippingAddress,
            BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal shippingFee,
            String couponCode, OrderStatus status, String note,
            List<OrderItemResponse> items, LocalDateTime createdAt, LocalDateTime updatedAt
    ) {
        this(id, userId, username, userEmail, shippingAddress, totalAmount, discountAmount, shippingFee,
             couponCode, status, PaymentMethod.COD, PaymentStatus.UNPAID, totalAmount != null ? totalAmount : BigDecimal.ZERO,
             BigDecimal.ZERO, null, note, items, createdAt, updatedAt);
    }
}
