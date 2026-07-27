package com.training.marketplace.dto.response;

import java.time.LocalDateTime;

public record StockLevelResponse(
        Long id,
        Long variantId,
        String sku,
        String variantName,
        Long warehouseId,
        String warehouseCode,
        String warehouseName,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Integer minStock,
        Integer maxStock,
        Integer reorderPoint,
        Long version,
        LocalDateTime updatedAt
) {}
