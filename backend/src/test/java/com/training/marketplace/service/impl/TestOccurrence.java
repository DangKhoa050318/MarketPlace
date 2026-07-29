package com.training.marketplace.service.impl;

import com.training.marketplace.repository.projection.ProductCoOccurrenceProjection;

record TestOccurrence(
        Long productId,
        Long coOccurrenceCount
) implements ProductCoOccurrenceProjection {

    @Override
    public Long getProductId() {
        return productId;
    }

    @Override
    public Long getCoOccurrenceCount() {
        return coOccurrenceCount;
    }
}
