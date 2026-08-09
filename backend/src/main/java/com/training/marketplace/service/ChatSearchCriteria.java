package com.training.marketplace.service;

import java.math.BigDecimal;
import java.util.Set;

public record ChatSearchCriteria(
        String query,
        String category,
        Long pageCategoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String brand,
        Set<Long> productScopeIds,
        Set<Long> categoryScopeIds,
        boolean offerOnly,
        boolean cartWideOffer,
        int limit
) {
    public ChatSearchCriteria {
        productScopeIds = productScopeIds == null ? Set.of() : Set.copyOf(productScopeIds);
        categoryScopeIds = categoryScopeIds == null ? Set.of() : Set.copyOf(categoryScopeIds);
    }
}
