package com.training.marketplace.repository.projection;

import java.time.LocalDateTime;

public interface StockLevelProjection {

    Long getId();

    Long getVariantId();

    String getSku();

    String getVariantName();

    Long getWarehouseId();

    String getWarehouseCode();

    String getWarehouseName();

    Integer getQuantity();

    Integer getReservedQuantity();

    Integer getAvailableQuantity();

    Integer getMinStock();

    Integer getMaxStock();

    Integer getReorderPoint();

    Long getVersion();

    LocalDateTime getUpdatedAt();
}
