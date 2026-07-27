package com.training.marketplace.repository.projection;

public interface StockThresholdProjection {

    Long getVariantId();

    Long getWarehouseId();

    Integer getQuantity();

    Integer getReorderPoint();
}
