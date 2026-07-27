package com.training.marketplace.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SPU-level product view. On detail (getById) the {@code variants} list is populated;
 * on list endpoints it is left null.
 */
public record ProductResponse(
        Long id,
        String slug,
        String name,
        String description,
        Long categoryId,
        String unit,
        String imageUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ProductVariantResponse> variants
) {
    public ProductResponse withVariants(List<ProductVariantResponse> variants) {
        return new ProductResponse(id, slug, name, description, categoryId, unit, imageUrl,
                active, createdAt, updatedAt, variants);
    }
}
