package com.training.marketplace.service;

/**
 * Internal, transport-independent input shared by recommendation strategies.
 *
 * <p>Context fields are optional because different strategies need different inputs:
 * similar and co-occurrence strategies use a source product, category best sellers use
 * a category, and later personalized strategies may use user or session identity.</p>
 */
public record RecommendationContext(
        Long userId,
        String sessionId,
        Long productId,
        Long categoryId,
        int limit
) {

    public RecommendationContext {
        requirePositiveWhenPresent(userId, "User ID");
        requirePositiveWhenPresent(productId, "Product ID");
        requirePositiveWhenPresent(categoryId, "Category ID");
        if (limit <= 0) {
            throw new IllegalArgumentException("Recommendation limit must be positive");
        }
    }

    private static void requirePositiveWhenPresent(Long value, String fieldName) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }
}
