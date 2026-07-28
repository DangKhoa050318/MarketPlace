package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StorefrontProductResponse(
        Long id,
        String slug,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String unit,
        String imageUrl,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        long availableStock,
        long variantCount,
        LocalDateTime createdAt
) {}
