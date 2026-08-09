package com.training.marketplace.service;

import java.math.BigDecimal;

public record ChatProductCandidate(
        Long productId,
        String slug,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String brand,
        String imageUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        long availableStock
) {
}
