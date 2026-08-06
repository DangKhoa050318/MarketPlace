package com.training.marketplace.service;

import java.util.Map;

/**
 * Boundary between ordering and inventory. Ordering never touches stock levels directly.
 */
public interface InventoryFacade {

    /** Warehouse used to fulfil storefront orders (MVP: first active warehouse). */
    Long defaultWarehouseId();

    void reserve(Long warehouseId, Map<Long, Integer> quantityByVariant);

    /** Release a previous reservation (for example on order cancel). */
    void release(Long warehouseId, Map<Long, Integer> quantityByVariant);

    /** Convert a reservation into an actual decrement, usually when the order ships. */
    void fulfill(Long warehouseId, Map<Long, Integer> quantityByVariant);

    /** Add stock back on-hand after a fulfilled order is returned or refused. */
    void returnStock(Long warehouseId, Map<Long, Integer> quantityByVariant);

    /** Add accepted returned goods back to on-hand stock after QC passes. */
    void restockReturn(Long warehouseId, Map<Long, Integer> quantityByVariant);
}
