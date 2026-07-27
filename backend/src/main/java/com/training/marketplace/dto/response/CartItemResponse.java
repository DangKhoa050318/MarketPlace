package com.training.marketplace.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        Long variantId,
        String sku,
        String productName,
        String variantName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        String imageUrl
) {}
