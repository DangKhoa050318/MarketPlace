package com.training.marketplace.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderCreatedEvent(
        String eventId,
        Long orderId,
        Long userId,
        String userEmail,
        Long warehouseId,
        BigDecimal totalAmount,
        LocalDateTime eventTime,
        List<OrderItemInfo> items
) {
    public record OrderItemInfo(
            Long variantId,
            String sku,
            String productName,
            BigDecimal unitPrice,
            Integer quantity,
            BigDecimal subtotal
    ) {}
}
