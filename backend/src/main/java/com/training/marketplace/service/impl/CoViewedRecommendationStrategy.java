package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.config.RecommendationProperties;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.repository.projection.ProductCoOccurrenceProjection;
import com.training.marketplace.service.RecommendationCandidate;
import com.training.marketplace.service.RecommendationContext;
import com.training.marketplace.service.RecommendationStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Ranks products viewed by the same distinct browsing actors as the source product.
 */
@Service
public class CoViewedRecommendationStrategy implements RecommendationStrategy {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final RecommendationSourceProductValidator sourceProductValidator;
    private final RecommendationProperties properties;
    private final Clock clock;

    @Autowired
    public CoViewedRecommendationStrategy(
            AnalyticsEventRepository analyticsEventRepository,
            RecommendationSourceProductValidator sourceProductValidator,
            RecommendationProperties properties) {
        this(
                analyticsEventRepository,
                sourceProductValidator,
                properties,
                Clock.systemUTC());
    }

    CoViewedRecommendationStrategy(
            AnalyticsEventRepository analyticsEventRepository,
            RecommendationSourceProductValidator sourceProductValidator,
            RecommendationProperties properties,
            Clock clock) {
        this.analyticsEventRepository = analyticsEventRepository;
        this.sourceProductValidator = sourceProductValidator;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public RecommendationStrategyType getType() {
        return RecommendationStrategyType.CO_VIEWED;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationCandidate> recommend(RecommendationContext context) {
        sourceProductValidator.requireActive(context.productId(), getType());
        Duration lookback = properties.getCoViewedLookback();
        int minimumOccurrences = properties.getMinCoViewedOccurrences();
        validateConfiguration(lookback, minimumOccurrences);

        Instant since = clock.instant().minus(lookback);
        List<ProductCoOccurrenceProjection> aggregates =
                analyticsEventRepository.findCoViewedProducts(
                        context.productId(),
                        since,
                        minimumOccurrences,
                        PageRequest.of(0, context.limit()));

        return aggregates.stream()
                .map(aggregate -> new RecommendationCandidate(
                        aggregate.getProductId(),
                        aggregate.getCoOccurrenceCount().doubleValue(),
                        "co_viewed_actors=" + aggregate.getCoOccurrenceCount()
                                + ",window=" + lookback))
                .toList();
    }

    private void validateConfiguration(Duration lookback, int minimumOccurrences) {
        if (lookback == null || lookback.compareTo(Duration.ofDays(1)) < 0) {
            throw new IllegalStateException("Co-viewed lookback must be at least one day");
        }
        if (minimumOccurrences < 2) {
            throw new IllegalStateException(
                    "Co-viewed minimum occurrences must be at least 2");
        }
    }
}
