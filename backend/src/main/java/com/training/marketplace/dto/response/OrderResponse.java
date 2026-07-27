package com.training.marketplace.dto.response;

import com.training.marketplace.enums.OrderStatus;
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
        OrderStatus status,
        String note,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
