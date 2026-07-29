package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.entity.Product;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.projection.ProductPriceRangeProjection;
import com.training.marketplace.service.RecommendationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarProductRecommendationStrategyTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private SimilarProductRecommendationStrategy strategy;

    @Test
    void getType_returnsSimilar() {
        assertThat(strategy.getType()).isEqualTo(RecommendationStrategyType.SIMILAR);
    }

    @Test
    void recommend_ranksByCategoryBrandAttributesAndPriceRange() {
        Product source = product(
                10L, 1L, " Acme ", Map.of("Type", "Laptop", "RAM", "16 GB"), true);
        Product strongest = product(
                20L, 1L, "acme", Map.of("type", "laptop", "ram", "16 gb"), true);
        Product brandAndAttribute = product(
                30L, 2L, "ACME", Map.of("ram", "16 GB"), true);
        Product categoryOnly = product(
                40L, 1L, "Other", Map.of("type", "tablet"), true);
        Product unrelated = product(
                50L, 3L, "Unrelated", Map.of("type", "camera"), true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(source));
        when(productRepository.findAllByActiveTrue())
                .thenReturn(List.of(source, categoryOnly, unrelated, strongest, brandAndAttribute));
        when(productVariantRepository.findActivePriceRanges(anyCollection())).thenReturn(List.of(
                priceRange(10L, "900.00", "1100.00"),
                priceRange(20L, "1000.00", "1200.00"),
                priceRange(30L, "950.00", "1050.00"),
                priceRange(40L, "900.00", "1000.00"),
                priceRange(50L, "990.00", "1010.00")));

        var result = strategy.recommend(context(10L, 10));

        assertThat(result)
                .extracting(candidate -> candidate.productId())
                .containsExactly(20L, 30L, 40L);
        assertThat(result.get(0).score()).isGreaterThan(result.get(1).score());
        assertThat(result.get(1).score()).isGreaterThan(result.get(2).score());
        assertThat(result.get(0).reason())
                .contains("same_category", "same_brand", "matching_attributes", "similar_price");
        assertThat(result).noneMatch(candidate -> candidate.productId().equals(10L));
        assertThat(result).noneMatch(candidate -> candidate.productId().equals(50L));
    }

    @Test
    void recommend_appliesLimitAndDeterministicProductIdTieBreak() {
        Product source = product(10L, 1L, null, Map.of(), true);
        Product higherId = product(30L, 1L, null, Map.of(), true);
        Product lowerId = product(20L, 1L, null, Map.of(), true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(source));
        when(productRepository.findAllByActiveTrue())
                .thenReturn(List.of(source, higherId, lowerId));
        when(productVariantRepository.findActivePriceRanges(anyCollection()))
                .thenReturn(List.of());

        var result = strategy.recommend(context(10L, 1));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).productId()).isEqualTo(20L);
    }

    @Test
    void recommend_ignoresInactiveCandidateEvenIfRepositoryReturnsIt() {
        Product source = product(10L, 1L, "Acme", Map.of(), true);
        Product inactive = product(20L, 1L, "Acme", Map.of(), false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(source));
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(source, inactive));
        when(productVariantRepository.findActivePriceRanges(anyCollection()))
                .thenReturn(List.of());

        assertThat(strategy.recommend(context(10L, 10))).isEmpty();
    }

    @Test
    void recommend_requiresSourceProductId() {
        RecommendationContext context =
                new RecommendationContext(null, "session-1", null, null, 10);

        assertThatThrownBy(() -> strategy.recommend(context))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Source product ID");
        verifyNoInteractions(productRepository, productVariantRepository);
    }

    @Test
    void recommend_rejectsMissingOrInactiveSource() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategy.recommend(context(99L, 10)))
                .isInstanceOf(ResourceNotFoundException.class);

        Product inactive = product(10L, 1L, "Acme", Map.of(), false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> strategy.recommend(context(10L, 10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active");
    }

    private RecommendationContext context(Long productId, int limit) {
        return new RecommendationContext(null, "session-1", productId, null, limit);
    }

    private Product product(
            Long id,
            Long categoryId,
            String brand,
            Map<String, String> attributes,
            boolean active) {
        Product product = Product.builder()
                .slug("product-" + id)
                .name("Product " + id)
                .categoryId(categoryId)
                .brand(brand)
                .attributes(attributes)
                .active(active)
                .build();
        product.setId(id);
        return product;
    }

    private ProductPriceRangeProjection priceRange(
            Long productId,
            String minPrice,
            String maxPrice) {
        return new ProductPriceRangeProjection() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public BigDecimal getMinPrice() {
                return new BigDecimal(minPrice);
            }

            @Override
            public BigDecimal getMaxPrice() {
                return new BigDecimal(maxPrice);
            }
        };
    }
}
