package com.training.marketplace.analytics;

/**
 * The customer journey source that led to an analytics event.
 *
 * <p>The value is deliberately coarse-grained and contains no user-identifying data.
 * Recommendation-specific details belong in {@link RecommendationPlacement} and
 * {@link RecommendationStrategyType}.</p>
 */
public enum AnalyticsEventSource {
    DIRECT,
    CATALOG,
    SEARCH,
    RECENTLY_VIEWED,
    RECOMMENDATION,
    CART,
    CHECKOUT,
    ORDER_SERVICE
}
