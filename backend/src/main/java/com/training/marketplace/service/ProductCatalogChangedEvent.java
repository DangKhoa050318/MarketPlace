package com.training.marketplace.service;

import java.util.Objects;

public record ProductCatalogChangedEvent(Long productId) {

    public ProductCatalogChangedEvent {
        Objects.requireNonNull(productId, "Product ID is required");
    }
}
