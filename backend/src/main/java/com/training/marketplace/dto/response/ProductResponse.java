package com.training.marketplace.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
        String brand,
        Map<String, String> attributes,
        String unit,
        String imageUrl,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<ProductVariantResponse> variants
) {
    public ProductResponse {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public ProductResponse(
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
            List<ProductVariantResponse> variants) {
        this(id, slug, name, description, categoryId, null, Map.of(), unit, imageUrl,
                active, createdAt, updatedAt, variants);
    }

    public ProductResponse withVariants(List<ProductVariantResponse> variants) {
        return new ProductResponse(id, slug, name, description, categoryId, brand, attributes,
                unit, imageUrl, active, createdAt, updatedAt, variants);
    }
}
