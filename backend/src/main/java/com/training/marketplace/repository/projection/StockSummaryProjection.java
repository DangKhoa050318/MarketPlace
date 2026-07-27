package com.training.marketplace.repository.projection;

public interface StockSummaryProjection {

    Long getVariantId();

    String getSku();

    String getVariantName();

    String getProductName();

    String getCategoryName();

    Long getWarehouseId();

    String getWarehouseName();

    Integer getQuantity();

    Integer getReservedQuantity();

    Integer getAvailableQuantity();

    Integer getMinStock();

    Integer getReorderPoint();

    String getStockStatus();
}
