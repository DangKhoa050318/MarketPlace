package com.training.marketplace.service;

import com.training.marketplace.analytics.RecommendationStrategyType;

import java.util.List;

/**
 * Pluggable recommendation algorithm contract.
 *
 * <p>Implementations own candidate selection and ranking only. Controllers, HTTP DTOs,
 * product response mapping, and final eligibility filtering must remain outside a strategy.</p>
 */
public interface RecommendationStrategy {

    /**
     * Stable identifier used to register and resolve this implementation.
     */
    RecommendationStrategyType getType();

    /**
     * Returns candidates in descending recommendation order.
     *
     * @return a non-null list; use an empty list when no candidate is available
     */
    List<RecommendationCandidate> recommend(RecommendationContext context);
}
