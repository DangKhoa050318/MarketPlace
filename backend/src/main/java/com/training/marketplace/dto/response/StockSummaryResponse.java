package com.training.marketplace.dto.response;

public record StockSummaryResponse(
        Long variantId,
        String sku,
        String variantName,
        String productName,
        String categoryName,
        Long warehouseId,
        String warehouseName,
        Integer quantity,
        Integer reservedQuantity,
        Integer availableQuantity,
        Integer minStock,
        Integer reorderPoint,
        String stockStatus) {
}
