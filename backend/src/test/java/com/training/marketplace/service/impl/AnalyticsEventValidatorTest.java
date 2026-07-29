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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsEventValidatorTest {

    private static final Instant NOW = Instant.parse("2026-07-28T12:30:00Z");

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    private AnalyticsEventValidator validator;

    @BeforeEach
    void setUp() {
        validator = new AnalyticsEventValidator(
                productRepository,
                productVariantRepository,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void validate_acceptsCompleteRecommendationContext() {
        Product product = activeProduct(10L);
        ProductVariant variant = activeVariant(20L, 10L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(20L)).thenReturn(Optional.of(variant));
        TrackAnalyticsEventRequest request = recommendationRequest(
                RecommendationPlacement.PRODUCT_DETAIL_SIMILAR,
                RecommendationStrategyType.SIMILAR);

        assertThatCode(() -> validator.validate(request, 10L, 20L, null, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_acceptsCoPurchasedPlacementWithMatchingStrategy() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(activeProduct(10L)));
        when(productVariantRepository.findById(20L))
                .thenReturn(Optional.of(activeVariant(20L, 10L)));
        TrackAnalyticsEventRequest request = recommendationRequest(
                RecommendationPlacement.PRODUCT_DETAIL_CO_PURCHASED,
                RecommendationStrategyType.CO_PURCHASED);

        assertThatCode(() -> validator.validate(request, 10L, 20L, null, NOW))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_rejectsMissingProductForProductEvent() {
        TrackAnalyticsEventRequest request = basicRequest(
                AnalyticsEventType.PRODUCT_VIEW,
                NOW,
                null,
                null);

        assertThatThrownBy(() -> validator.validate(request, null, null, null, NOW))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Product ID is required");
    }

    @Test
    void validate_rejectsUnknownProduct() {
        TrackAnalyticsEventRequest request = basicRequest(
                AnalyticsEventType.PRODUCT_VIEW,
                NOW,
                999L,
                null);
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validator.validate(request, 999L, null, null, NOW))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid product ID");
    }

    @Test
    void validate_rejectsVariantThatBelongsToAnotherProduct() {
        Product product = activeProduct(10L);
        ProductVariant variant = activeVariant(20L, 11L);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(20L)).thenReturn(Optional.of(variant));
        TrackAnalyticsEventRequest request = basicRequest(
                AnalyticsEventType.ADD_TO_CART,
                NOW,
                10L,
                20L);

        assertThatThrownBy(() -> validator.validate(request, 10L, 20L, 1, NOW))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong to product");
    }

    @Test
    void validate_rejectsTimestampOutsideIngestionWindow() {
        TrackAnalyticsEventRequest futureRequest = basicRequest(
                AnalyticsEventType.PAGE_VIEW,
                NOW.plus(AnalyticsEventValidator.MAX_FUTURE_SKEW).plusSeconds(1),
                null,
                null);
        TrackAnalyticsEventRequest oldRequest = basicRequest(
                AnalyticsEventType.PAGE_VIEW,
                NOW.minus(AnalyticsEventValidator.MAX_EVENT_AGE).minusSeconds(1),
                null,
                null);

        assertThatThrownBy(() -> validator.validate(
                futureRequest, null, null, null, futureRequest.occurredAt()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("future");
        assertThatThrownBy(() -> validator.validate(
                oldRequest, null, null, null, oldRequest.occurredAt()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("older");
    }

    @Test
    void validate_rejectsPlacementStrategyMismatch() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(activeProduct(10L)));
        when(productVariantRepository.findById(20L))
                .thenReturn(Optional.of(activeVariant(20L, 10L)));
        TrackAnalyticsEventRequest request = recommendationRequest(
                RecommendationPlacement.PRODUCT_DETAIL_SIMILAR,
                RecommendationStrategyType.BEST_SELLER);

        assertThatThrownBy(() -> validator.validate(request, 10L, 20L, null, NOW))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not valid for placement");
    }

    @Test
    void validate_rejectsBrowserSubmittedPurchase() {
        TrackAnalyticsEventRequest request = basicRequest(
                AnalyticsEventType.PURCHASE,
                NOW,
                10L,
                20L);

        assertThatThrownBy(() -> validator.validate(request, 10L, 20L, 1, NOW))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("server-generated");
    }

    @Test
    void validate_rejectsMissingEventTypeAtServiceBoundary() {
        TrackAnalyticsEventRequest request = basicRequest(null, NOW, null, null);

        assertThatThrownBy(() -> validator.validate(request, null, null, null, NOW))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Event type is required");
    }

    private TrackAnalyticsEventRequest recommendationRequest(
            RecommendationPlacement placement,
            RecommendationStrategyType strategy) {
        return new TrackAnalyticsEventRequest(
                UUID.randomUUID(),
                1,
                AnalyticsEventType.RECOMMENDATION_IMPRESSION,
                NOW,
                10L,
                20L,
                AnalyticsEventSource.RECOMMENDATION,
                placement,
                UUID.randomUUID(),
                strategy,
                0,
                null,
                null,
                null,
                Map.of());
    }

    private TrackAnalyticsEventRequest basicRequest(
            AnalyticsEventType type,
            Instant occurredAt,
            Long productId,
            Long variantId) {
        return new TrackAnalyticsEventRequest(
                UUID.randomUUID(),
                1,
                type,
                occurredAt,
                productId,
                variantId,
                null,
                null,
                null,
                null,
                null,
                type == AnalyticsEventType.ADD_TO_CART ? 1 : null,
                null,
                null,
                Map.of());
    }

    private Product activeProduct(Long id) {
        Product product = Product.builder()
                .slug("product-" + id)
                .name("Product " + id)
                .active(true)
                .build();
        product.setId(id);
        return product;
    }

    private ProductVariant activeVariant(Long id, Long productId) {
        ProductVariant variant = ProductVariant.builder()
                .productId(productId)
                .sku("SKU-" + id)
                .variantName("Variant " + id)
                .active(true)
                .build();
        variant.setId(id);
        return variant;
    }
}
