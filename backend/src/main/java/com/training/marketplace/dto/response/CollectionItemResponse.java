package com.training.marketplace.dto.response;

/** One product's slot in a collection, enriched with product display fields for storefront rendering. */
public record CollectionItemResponse(
        Long productId,
        Integer displayOrder,
        String productName,
        String productSlug,
        String imageUrl
) {}
