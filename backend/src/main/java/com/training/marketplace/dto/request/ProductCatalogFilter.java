package com.training.marketplace.dto.request;

import java.math.BigDecimal;

public record ProductCatalogFilter(
        String query,
        Long categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStock
) {}
