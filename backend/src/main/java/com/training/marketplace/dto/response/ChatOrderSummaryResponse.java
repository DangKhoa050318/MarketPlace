package com.training.marketplace.dto.response;

import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChatOrderSummaryResponse(
        Long id,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        String couponCode,
        String shippingAddress,
        String note,
        int itemCount,
        String paymentUrl,
        LocalDateTime paymentExpiresAt,
        LocalDateTime createdAt
) {
}
