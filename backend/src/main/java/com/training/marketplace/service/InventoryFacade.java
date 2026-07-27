package com.training.marketplace.service;

import java.util.Map;

/**
 * The one seam between the ordering module and the inventory module. Ordering never touches
 * stock_levels directly — it goes through here. Keeping this as an interface means a future
 * split into a separate inventory microservice only changes the implementation (in-process →
 * message/REST), not the callers.
 */
public interface InventoryFacade {

    /** Warehouse used to fulfil storefront orders (MVP: first active warehouse). */
    Long defaultWarehouseId();

    /**
     * Reserve stock for {@code variantId -> quantity} at a warehouse. Locks the affected
     * stock_levels rows (in variantId order) and throws {@code BadRequestException} if any
     * variant has insufficient available quantity. This is the authoritative no-oversell point.
     */
    void reserve(Long warehouseId, Map<Long, Integer> quantityByVariant);

    /** Release a previous reservation (e.g. on order cancel). */
    void release(Long warehouseId, Map<Long, Integer> quantityByVariant);

    /** Convert a reservation into an actual decrement (e.g. on shipment). */
    void fulfill(Long warehouseId, Map<Long, Integer> quantityByVariant);
}
