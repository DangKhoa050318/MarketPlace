package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** SKU-level view (a "mẫu mã" of a product). */
public record ProductVariantResponse(
        Long id,
        Long productId,
        String sku,
        String variantName,
        String color,
        String size,
        BigDecimal price,
        String imageUrl,
        Integer minStock,
        Integer maxStock,
        Integer reorderPoint,
        Integer reorderQuantity,
        boolean active,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
