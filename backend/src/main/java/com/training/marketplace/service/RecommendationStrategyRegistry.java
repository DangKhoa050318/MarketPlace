package com.training.marketplace.service;

import com.training.marketplace.analytics.RecommendationStrategyType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves recommendation algorithms without exposing concrete implementations to
 * controllers or application services.
 */
@Component
public class RecommendationStrategyRegistry {

    private final Map<RecommendationStrategyType, RecommendationStrategy> strategies;

    public RecommendationStrategyRegistry(
            List<RecommendationStrategy> strategies,
            RecommendationCandidateFilter candidateFilter) {
        Objects.requireNonNull(strategies, "Recommendation strategies are required");
        Objects.requireNonNull(candidateFilter, "Recommendation candidate filter is required");
        EnumMap<RecommendationStrategyType, RecommendationStrategy> registered =
                new EnumMap<>(RecommendationStrategyType.class);
        for (RecommendationStrategy strategy : strategies) {
            Objects.requireNonNull(strategy, "Recommendation strategy must not be null");
            RecommendationStrategyType type = Objects.requireNonNull(
                    strategy.getType(),
                    "Recommendation strategy type must not be null");
            RecommendationStrategy decorated =
                    new EligibilityFilteringStrategy(strategy, candidateFilter);
            RecommendationStrategy duplicate = registered.putIfAbsent(type, decorated);
            if (duplicate != null) {
                throw new IllegalStateException(
                        "Multiple recommendation strategies registered for type: " + type);
            }
        }
        this.strategies = Collections.unmodifiableMap(registered);
    }

    public RecommendationStrategy resolve(RecommendationStrategyType type) {
        Objects.requireNonNull(type, "Recommendation strategy type is required");
        RecommendationStrategy strategy = strategies.get(type);
        if (strategy == null) {
            throw new IllegalStateException(
                    "No recommendation strategy registered for type: " + type);
        }
        return strategy;
    }

    public boolean supports(RecommendationStrategyType type) {
        return type != null && strategies.containsKey(type);
    }

    public Set<RecommendationStrategyType> registeredTypes() {
        return strategies.keySet();
    }

    private record EligibilityFilteringStrategy(
            RecommendationStrategy delegate,
            RecommendationCandidateFilter candidateFilter
    ) implements RecommendationStrategy {

        @Override
        public RecommendationStrategyType getType() {
            return delegate.getType();
        }

        @Override
        public List<RecommendationCandidate> recommend(RecommendationContext context) {
            Objects.requireNonNull(context, "Recommendation context is required");
            return candidateFilter.filter(
                    delegate.recommend(context),
                    context.productId(),
                    context.limit());
        }
    }
}
