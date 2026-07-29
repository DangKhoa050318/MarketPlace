package com.training.marketplace.repository.projection;

/**
 * Product/SPU sales aggregation calculated from valid orders and their SKU lines.
 */
public interface BestSellerAggregateProjection {

    Long getProductId();

    Long getOrderCount();

    Long getUnitsSold();
}
