package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.config.RecommendationProperties;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.repository.OrderItemRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Ranks products that occur in the same distinct valid orders as the source product.
 */
@Service
public class CoPurchasedRecommendationStrategy implements RecommendationStrategy {

    private final OrderItemRepository orderItemRepository;
    private final RecommendationSourceProductValidator sourceProductValidator;
    private final RecommendationProperties properties;
    private final Clock clock;

    @Autowired
    public CoPurchasedRecommendationStrategy(
            OrderItemRepository orderItemRepository,
            RecommendationSourceProductValidator sourceProductValidator,
            RecommendationProperties properties) {
        this(
                orderItemRepository,
                sourceProductValidator,
                properties,
                Clock.systemDefaultZone());
    }

    CoPurchasedRecommendationStrategy(
            OrderItemRepository orderItemRepository,
            RecommendationSourceProductValidator sourceProductValidator,
            RecommendationProperties properties,
            Clock clock) {
        this.orderItemRepository = orderItemRepository;
        this.sourceProductValidator = sourceProductValidator;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public RecommendationStrategyType getType() {
        return RecommendationStrategyType.CO_PURCHASED;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationCandidate> recommend(RecommendationContext context) {
        sourceProductValidator.requireActive(context.productId(), getType());
        Duration lookback = properties.getCoPurchasedLookback();
        int minimumOccurrences = properties.getMinCoPurchasedOccurrences();
        Set<OrderStatus> validStatuses = properties.validOrderStatusesSnapshot();
        validateConfiguration(lookback, minimumOccurrences, validStatuses);

        LocalDateTime since = LocalDateTime.now(clock).minus(lookback);
        List<ProductCoOccurrenceProjection> aggregates =
                orderItemRepository.findCoPurchasedProducts(
                        context.productId(),
                        validStatuses,
                        since,
                        minimumOccurrences,
                        PageRequest.of(0, context.limit()));

        return aggregates.stream()
                .map(aggregate -> new RecommendationCandidate(
                        aggregate.getProductId(),
                        aggregate.getCoOccurrenceCount().doubleValue(),
                        "co_purchased_orders=" + aggregate.getCoOccurrenceCount()
                                + ",window=" + lookback))
                .toList();
    }

    private void validateConfiguration(
            Duration lookback,
            int minimumOccurrences,
            Set<OrderStatus> validStatuses) {
        if (lookback == null || lookback.compareTo(Duration.ofDays(1)) < 0) {
            throw new IllegalStateException("Co-purchased lookback must be at least one day");
        }
        if (minimumOccurrences < 2) {
            throw new IllegalStateException(
                    "Co-purchased minimum occurrences must be at least 2");
        }
        if (!properties.isValidOrderStatusConfiguration() || validStatuses.isEmpty()) {
            throw new IllegalStateException(
                    "Co-purchased valid statuses must exclude PENDING and CANCELLED");
        }
    }
}
