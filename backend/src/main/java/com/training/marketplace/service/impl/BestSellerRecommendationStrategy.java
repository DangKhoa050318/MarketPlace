package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.config.RecommendationProperties;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.repository.OrderItemRepository;
import com.training.marketplace.repository.projection.BestSellerAggregateProjection;
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
 * Ranks products by distinct valid order count inside a configurable lookback window.
 * Units sold are used as a deterministic secondary ordering by the repository query.
 */
@Service
public class BestSellerRecommendationStrategy implements RecommendationStrategy {

    private final OrderItemRepository orderItemRepository;
    private final RecommendationProperties properties;
    private final Clock clock;

    @Autowired
    public BestSellerRecommendationStrategy(
            OrderItemRepository orderItemRepository,
            RecommendationProperties properties) {
        this(orderItemRepository, properties, Clock.systemDefaultZone());
    }

    BestSellerRecommendationStrategy(
            OrderItemRepository orderItemRepository,
            RecommendationProperties properties,
            Clock clock) {
        this.orderItemRepository = orderItemRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public RecommendationStrategyType getType() {
        return RecommendationStrategyType.BEST_SELLER;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationCandidate> recommend(RecommendationContext context) {
        Duration lookback = properties.getBestSellerLookback();
        Set<OrderStatus> validStatuses = properties.validOrderStatusesSnapshot();
        if (lookback == null || lookback.isNegative() || lookback.isZero()) {
            throw new IllegalStateException("Best-seller lookback must be positive");
        }
        if (!properties.isValidOrderStatusConfiguration()) {
            throw new IllegalStateException(
                    "Best-seller valid statuses must exclude PENDING and CANCELLED");
        }

        LocalDateTime since = LocalDateTime.now(clock).minus(lookback);
        List<BestSellerAggregateProjection> aggregates =
                orderItemRepository.findBestSellingProducts(
                        validStatuses,
                        since,
                        context.categoryId(),
                        PageRequest.of(0, context.limit()));

        return aggregates.stream()
                .map(aggregate -> new RecommendationCandidate(
                        aggregate.getProductId(),
                        aggregate.getOrderCount().doubleValue(),
                        reason(aggregate, lookback)))
                .toList();
    }

    private String reason(BestSellerAggregateProjection aggregate, Duration lookback) {
        return "valid_orders=" + aggregate.getOrderCount()
                + ",units_sold=" + aggregate.getUnitsSold()
                + ",window=" + lookback;
    }
}
