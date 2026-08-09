package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.AnalyticsEventSource;
import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

@Component
public class AnalyticsEventValidator {

    static final Duration MAX_FUTURE_SKEW = Duration.ofMinutes(5);
    static final Duration MAX_EVENT_AGE = Duration.ofDays(7);

    private static final Set<AnalyticsEventType> PRODUCT_REQUIRED_EVENTS = EnumSet.of(
            AnalyticsEventType.PRODUCT_VIEW,
            AnalyticsEventType.ADD_TO_WISHLIST,
            AnalyticsEventType.ADD_TO_CART,
            AnalyticsEventType.RECOMMENDATION_IMPRESSION,
            AnalyticsEventType.RECOMMENDATION_CLICK,
            AnalyticsEventType.CHAT_PRODUCT_IMPRESSION,
            AnalyticsEventType.CHAT_PRODUCT_CLICK,
            AnalyticsEventType.CHAT_VOUCHER_CLICK);

    private static final Set<AnalyticsEventType> RECOMMENDATION_EVENTS = EnumSet.of(
            AnalyticsEventType.RECOMMENDATION_IMPRESSION,
            AnalyticsEventType.RECOMMENDATION_CLICK);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final Clock clock;

    @Autowired
    public AnalyticsEventValidator(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository) {
        this(productRepository, productVariantRepository, Clock.systemUTC());
    }

    AnalyticsEventValidator(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            Clock clock) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.clock = clock;
    }

    public void validate(
            TrackAnalyticsEventRequest request,
            Long productId,
            Long variantId,
            Integer quantity,
            Instant occurredAt) {
        validateEnvelope(request, occurredAt);
        validateProductContext(request.type(), productId, variantId, quantity);
        validateRecommendationContext(request);
    }

    private void validateEnvelope(TrackAnalyticsEventRequest request, Instant occurredAt) {
        if (request.type() == null) {
            throw new BadRequestException("Event type is required");
        }
        if (request.type() == AnalyticsEventType.PURCHASE) {
            throw new BadRequestException(
                    "PURCHASE is a trusted server-generated event and cannot be submitted by a browser");
        }
        if (request.schemaVersion() != TrackAnalyticsEventRequest.CURRENT_SCHEMA_VERSION) {
            throw new BadRequestException("Unsupported analytics schema version: " + request.schemaVersion());
        }

        Instant now = clock.instant();
        if (occurredAt.isAfter(now.plus(MAX_FUTURE_SKEW))) {
            throw new BadRequestException("Event timestamp is too far in the future");
        }
        if (occurredAt.isBefore(now.minus(MAX_EVENT_AGE))) {
            throw new BadRequestException("Event timestamp is older than the supported ingestion window");
        }
    }

    private void validateProductContext(
            AnalyticsEventType type,
            Long productId,
            Long variantId,
            Integer quantity) {
        if (PRODUCT_REQUIRED_EVENTS.contains(type) && productId == null) {
            throw new BadRequestException("Product ID is required for event type " + type);
        }

        Product product = null;
        if (productId != null) {
            product = productRepository.findById(productId)
                    .orElseThrow(() -> new BadRequestException("Invalid product ID: " + productId));
            if (!product.isActive()) {
                throw new BadRequestException("Product is not active: " + productId);
            }
        }

        if (type == AnalyticsEventType.ADD_TO_CART && variantId == null) {
            throw new BadRequestException("Variant ID is required for ADD_TO_CART");
        }
        if (type == AnalyticsEventType.ADD_TO_CART && (quantity == null || quantity <= 0)) {
            throw new BadRequestException("Positive quantity is required for ADD_TO_CART");
        }

        if (variantId != null) {
            ProductVariant variant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new BadRequestException("Invalid variant ID: " + variantId));
            if (!variant.isActive()) {
                throw new BadRequestException("Variant is not active: " + variantId);
            }
            if (product != null && !variant.getProductId().equals(product.getId())) {
                throw new BadRequestException(
                        "Variant " + variantId + " does not belong to product " + productId);
            }
        }
    }

    private void validateRecommendationContext(TrackAnalyticsEventRequest request) {
        boolean recommendationEvent = RECOMMENDATION_EVENTS.contains(request.type());
        boolean hasRecommendationContext = request.placement() != null
                || request.recommendationRequestId() != null
                || request.strategy() != null
                || request.position() != null;

        if (recommendationEvent || request.source() == AnalyticsEventSource.RECOMMENDATION) {
            if (request.source() != AnalyticsEventSource.RECOMMENDATION) {
                throw new BadRequestException("Recommendation events must use source RECOMMENDATION");
            }
            if (request.placement() == null
                    || request.recommendationRequestId() == null
                    || request.strategy() == null
                    || request.position() == null) {
                throw new BadRequestException(
                        "Recommendation placement, request ID, strategy, and position are required");
            }
            validatePlacementStrategy(request.placement(), request.strategy());
        } else if (hasRecommendationContext) {
            throw new BadRequestException(
                    "Recommendation context requires source RECOMMENDATION");
        }
    }

    private void validatePlacementStrategy(
            RecommendationPlacement placement,
            RecommendationStrategyType strategy) {
        RecommendationStrategyType expected = switch (placement) {
            case PRODUCT_DETAIL_SIMILAR -> RecommendationStrategyType.SIMILAR;
            case PRODUCT_DETAIL_CO_VIEWED -> RecommendationStrategyType.CO_VIEWED;
            case PRODUCT_DETAIL_CO_PURCHASED -> RecommendationStrategyType.CO_PURCHASED;
            case HOME_BEST_SELLERS, CATEGORY_BEST_SELLERS ->
                    RecommendationStrategyType.BEST_SELLER;
        };
        if (strategy != expected) {
            throw new BadRequestException(
                    "Strategy " + strategy + " is not valid for placement " + placement);
        }
    }
}
