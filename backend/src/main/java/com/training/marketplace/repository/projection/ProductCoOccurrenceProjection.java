package com.training.marketplace.repository.projection;

/**
 * Product/SPU co-occurrence count produced from view sessions or valid orders.
 */
public interface ProductCoOccurrenceProjection {

    Long getProductId();

    Long getCoOccurrenceCount();
}
