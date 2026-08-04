package com.training.marketplace.dto.response;

/**
 * Lightweight per-product rating aggregate used to show star ratings on storefront
 * product cards / listings (G1). Batched by product id to avoid N+1 lookups.
 */
public record ProductRatingSummaryResponse(
        Long productId,
        double averageRating,
        long reviewCount
) {}
