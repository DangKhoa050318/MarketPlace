package com.training.marketplace.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long variantId,
        Long productId,
        String sku,
        String productName,
        String variantName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal
) {
}
