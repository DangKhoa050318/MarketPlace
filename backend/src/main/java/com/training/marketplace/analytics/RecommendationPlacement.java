package com.training.marketplace.analytics;

/**
 * Stable identifiers for the storefront locations that render recommendations.
 * These values form part of the analytics contract and must not be reused for a
 * semantically different placement.
 */
public enum RecommendationPlacement {
    PRODUCT_DETAIL_SIMILAR,
    PRODUCT_DETAIL_CO_VIEWED,
    PRODUCT_DETAIL_CO_PURCHASED,
    HOME_BEST_SELLERS,
    CATEGORY_BEST_SELLERS
}
