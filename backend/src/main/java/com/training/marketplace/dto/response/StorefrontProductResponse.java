package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime createdAt,
        String variantNames,
        List<StorefrontVariantItem> variants
) {
    public StorefrontProductResponse withVariants(List<StorefrontVariantItem> variants) {
        return new StorefrontProductResponse(
                id, slug, name, description, categoryId, categoryName, unit, imageUrl,
                minPrice, maxPrice, availableStock, variantCount, createdAt, variantNames, variants
        );
    }

    public record StorefrontVariantItem(
            Long id,
            String variantName,
            BigDecimal price,
            String imageUrl
    ) {}
}
