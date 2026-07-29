package com.training.marketplace.service;

import java.util.Objects;

/**
 * Ranked internal output from a recommendation algorithm.
 *
 * <p>Strategies return product identifiers rather than API DTOs so product hydration,
 * eligibility filtering, and response mapping remain outside the algorithm.</p>
 */
public record RecommendationCandidate(
        Long productId,
        double score,
        String reason
) {

    public RecommendationCandidate {
        Objects.requireNonNull(productId, "Product ID is required");
        if (productId <= 0) {
            throw new IllegalArgumentException("Product ID must be positive");
        }
        if (!Double.isFinite(score)) {
            throw new IllegalArgumentException("Recommendation score must be finite");
        }
    }
}
