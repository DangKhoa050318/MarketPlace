package com.training.marketplace.service;

import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.dto.response.ProductVariantResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.mapper.ProductMapper;
import com.training.marketplace.mapper.ProductVariantMapper;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationStrategyRegistry strategyRegistry;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductVariantMapper variantMapper;
    @Mock
    private RecommendationStrategy strategy;

    private RecommendationService service;

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(
                strategyRegistry,
                productRepository,
                variantRepository,
                productMapper,
                variantMapper);
    }

    @Test
    void recommend_mapsPlacementHydratesProductsAndPreservesCandidateOrder() {
        when(strategyRegistry.resolve(RecommendationStrategyType.SIMILAR))
                .thenReturn(strategy);
        when(strategy.recommend(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        new RecommendationCandidate(20L, 0.9, "same_brand"),
                        new RecommendationCandidate(30L, 0.7, "same_category")));

        Product product20 = product(20L, "first");
        Product product30 = product(30L, "second");
        when(productRepository.findAllByIdInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(product30, product20));
        ProductVariant variant20 = variant(201L, 20L);
        ProductVariant variant30 = variant(301L, 30L);
        when(variantRepository.findByProductIdInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(variant30, variant20));
        when(productMapper.toResponse(product20)).thenReturn(productResponse(20L, "first"));
        when(productMapper.toResponse(product30)).thenReturn(productResponse(30L, "second"));
        when(variantMapper.toResponse(variant20)).thenReturn(variantResponse(201L, 20L));
        when(variantMapper.toResponse(variant30)).thenReturn(variantResponse(301L, 30L));

        var response = service.recommend(
                7L,
                "  session-1  ",
                RecommendationPlacement.PRODUCT_DETAIL_SIMILAR,
                10L,
                99L,
                12);

        assertThat(response.requestId()).isNotNull();
        assertThat(response.generatedAt()).isNotNull();
        assertThat(response.placement())
                .isEqualTo(RecommendationPlacement.PRODUCT_DETAIL_SIMILAR);
        assertThat(response.strategy()).isEqualTo(RecommendationStrategyType.SIMILAR);
        assertThat(response.items())
                .extracting(item -> item.product().id())
                .containsExactly(20L, 30L);
        assertThat(response.items())
                .extracting(item -> item.position())
                .containsExactly(0, 1);
        assertThat(response.items().get(0).product().variants())
                .extracting(ProductVariantResponse::id)
                .containsExactly(201L);

        ArgumentCaptor<RecommendationContext> contextCaptor =
                ArgumentCaptor.forClass(RecommendationContext.class);
        verify(strategy).recommend(contextCaptor.capture());
        assertThat(contextCaptor.getValue())
                .isEqualTo(new RecommendationContext(7L, "session-1", 10L, null, 12));
    }

    @Test
    void recommend_categoryPlacementUsesBestSellerAndCategoryOnly() {
        when(strategyRegistry.resolve(RecommendationStrategyType.BEST_SELLER))
                .thenReturn(strategy);
        when(strategy.recommend(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        service.recommend(
                null,
                null,
                RecommendationPlacement.CATEGORY_BEST_SELLERS,
                99L,
                4L,
                6);

        ArgumentCaptor<RecommendationContext> contextCaptor =
                ArgumentCaptor.forClass(RecommendationContext.class);
        verify(strategy).recommend(contextCaptor.capture());
        assertThat(contextCaptor.getValue())
                .isEqualTo(new RecommendationContext(null, null, null, 4L, 6));
        verifyNoInteractions(productRepository, variantRepository);
    }

    @Test
    void recommend_dropsProductThatBecomesIneligibleDuringHydration() {
        when(strategyRegistry.resolve(RecommendationStrategyType.CO_VIEWED))
                .thenReturn(strategy);
        when(strategy.recommend(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(
                        new RecommendationCandidate(20L, 5.0, "eligible"),
                        new RecommendationCandidate(30L, 4.0, "became_inactive")));
        Product product20 = product(20L, "eligible");
        when(productRepository.findAllByIdInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(product20));
        ProductVariant variant20 = variant(201L, 20L);
        when(variantRepository.findByProductIdInAndActiveTrue(anyCollection()))
                .thenReturn(List.of(variant20));
        when(productMapper.toResponse(product20)).thenReturn(productResponse(20L, "eligible"));
        when(variantMapper.toResponse(variant20)).thenReturn(variantResponse(201L, 20L));

        var response = service.recommend(
                null,
                null,
                RecommendationPlacement.PRODUCT_DETAIL_CO_VIEWED,
                10L,
                null,
                5);

        assertThat(response.items())
                .extracting(item -> item.product().id())
                .containsExactly(20L);
        assertThat(response.items().get(0).position()).isZero();
    }

    @Test
    void recommend_emptyCandidatesReturnsTrackingContextWithoutHydrationQueries() {
        when(strategyRegistry.resolve(RecommendationStrategyType.BEST_SELLER))
                .thenReturn(strategy);
        when(strategy.recommend(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());

        var response = service.recommend(
                null,
                null,
                RecommendationPlacement.HOME_BEST_SELLERS,
                null,
                null,
                12);

        assertThat(response.items()).isEmpty();
        assertThat(response.requestId()).isNotNull();
        verify(productRepository, never()).findAllByIdInAndActiveTrue(anyCollection());
        verify(variantRepository, never()).findByProductIdInAndActiveTrue(anyCollection());
    }

    @Test
    void recommend_rejectsMissingPlacementIdentifiersInvalidLimitAndLongSession() {
        assertThatThrownBy(() -> service.recommend(
                null, null, null, null, null, 12))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("placement");
        assertThatThrownBy(() -> service.recommend(
                null,
                null,
                RecommendationPlacement.PRODUCT_DETAIL_CO_PURCHASED,
                null,
                null,
                12))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Product ID");
        assertThatThrownBy(() -> service.recommend(
                null,
                null,
                RecommendationPlacement.CATEGORY_BEST_SELLERS,
                null,
                null,
                12))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Category ID");
        assertThatThrownBy(() -> service.recommend(
                null,
                null,
                RecommendationPlacement.HOME_BEST_SELLERS,
                null,
                null,
                25))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("between 1 and 24");
        assertThatThrownBy(() -> service.recommend(
                null,
                "s".repeat(129),
                RecommendationPlacement.HOME_BEST_SELLERS,
                null,
                null,
                12))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("128");

        verifyNoInteractions(strategyRegistry);
    }

    private Product product(Long id, String slug) {
        Product product = Product.builder()
                .slug(slug)
                .name(slug)
                .active(true)
                .build();
        product.setId(id);
        return product;
    }

    private ProductVariant variant(Long id, Long productId) {
        ProductVariant variant = ProductVariant.builder()
                .productId(productId)
                .sku("SKU-" + id)
                .variantName("Default")
                .price(BigDecimal.TEN)
                .active(true)
                .build();
        variant.setId(id);
        return variant;
    }

    private ProductResponse productResponse(Long id, String slug) {
        return new ProductResponse(
                id,
                slug,
                slug,
                "description",
                1L,
                "PCS",
                null,
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null);
    }

    private ProductVariantResponse variantResponse(Long id, Long productId) {
        return new ProductVariantResponse(
                id,
                productId,
                "SKU-" + id,
                "Default",
                null,
                null,
                BigDecimal.TEN,
                null,
                1,
                10,
                2,
                5,
                true,
                0L,
                LocalDateTime.now(),
                LocalDateTime.now());
    }
}
