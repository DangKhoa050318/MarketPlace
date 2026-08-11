package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.entity.Product;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.service.RecommendationCandidate;
import com.training.marketplace.service.RecommendationContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarProductRecommendationStrategyTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SimilarProductPrecomputeService precomputeService;

    @InjectMocks
    private SimilarProductRecommendationStrategy strategy;

    @Test
    void getType_returnsSimilar() {
        assertThat(strategy.getType()).isEqualTo(RecommendationStrategyType.SIMILAR);
    }

    @Test
    void recommend_readsBoundedPrecomputedResultsWithoutScanningCatalog() {
        Product source = product(10L, true);
        List<RecommendationCandidate> precomputed = List.of(
                new RecommendationCandidate(20L, 1.0, "same_category,same_brand"),
                new RecommendationCandidate(30L, 0.6, "matching_attributes"));
        when(productRepository.findById(10L)).thenReturn(Optional.of(source));
        when(precomputeService.getOrCompute(10L, 2)).thenReturn(precomputed);

        assertThat(strategy.recommend(context(10L, 2))).isEqualTo(precomputed);

        verify(productRepository).findById(10L);
        verify(precomputeService).getOrCompute(10L, 2);
    }

    @Test
    void recommend_requiresSourceProductId() {
        RecommendationContext context =
                new RecommendationContext(null, "session-1", null, null, 10);

        assertThatThrownBy(() -> strategy.recommend(context))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Source product ID");
        verifyNoInteractions(productRepository, precomputeService);
    }

    @Test
    void recommend_rejectsMissingOrInactiveSourceWithoutPrecomputing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategy.recommend(context(99L, 10)))
                .isInstanceOf(ResourceNotFoundException.class);

        Product inactive = product(10L, false);
        when(productRepository.findById(10L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> strategy.recommend(context(10L, 10)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active");
        verifyNoInteractions(precomputeService);
    }

    private RecommendationContext context(Long productId, int limit) {
        return new RecommendationContext(null, "session-1", productId, null, limit);
    }

    private Product product(Long id, boolean active) {
        Product product = Product.builder()
                .slug("product-" + id)
                .name("Product " + id)
                .active(active)
                .build();
        product.setId(id);
        return product;
    }
}
