package com.training.marketplace.service;

import com.training.marketplace.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationCandidateFilterTest {

    @Mock
    private ProductRepository productRepository;

    private RecommendationCandidateFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RecommendationCandidateFilter(productRepository);
    }

    @Test
    void filter_removesSourceIneligibleAndDuplicateProductsWhilePreservingRank() {
        List<RecommendationCandidate> candidates = List.of(
                candidate(10L, 10.0),
                candidate(20L, 9.0),
                candidate(30L, 8.0),
                candidate(20L, 7.0),
                candidate(40L, 6.0),
                candidate(50L, 5.0));
        when(productRepository.findRecommendationEligibleProductIds(anyCollection()))
                .thenReturn(List.of(20L, 50L));

        List<RecommendationCandidate> result = filter.filter(candidates, 10L, 10);

        assertThat(result)
                .extracting(RecommendationCandidate::productId)
                .containsExactly(20L, 50L);
        assertThat(result.get(0).score()).isEqualTo(9.0);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<Long>> idsCaptor =
                ArgumentCaptor.forClass(Collection.class);
        verify(productRepository).findRecommendationEligibleProductIds(idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(20L, 30L, 40L, 50L);
    }

    @Test
    void filter_appliesLimitAfterEligibilityFiltering() {
        List<RecommendationCandidate> candidates = List.of(
                candidate(20L, 9.0),
                candidate(30L, 8.0),
                candidate(40L, 7.0));
        when(productRepository.findRecommendationEligibleProductIds(anyCollection()))
                .thenReturn(List.of(20L, 30L, 40L));

        assertThat(filter.filter(candidates, null, 2))
                .extracting(RecommendationCandidate::productId)
                .containsExactly(20L, 30L);
    }

    @Test
    void filter_returnsEmptyWithoutQueryWhenThereAreNoCandidates() {
        assertThat(filter.filter(List.of(), 10L, 5)).isEmpty();

        verify(productRepository, never())
                .findRecommendationEligibleProductIds(anyCollection());
    }

    @Test
    void filter_returnsEmptyWithoutQueryWhenOnlySourceWasReturned() {
        assertThat(filter.filter(List.of(candidate(10L, 1.0)), 10L, 5)).isEmpty();

        verify(productRepository, never())
                .findRecommendationEligibleProductIds(anyCollection());
    }

    @Test
    void filter_rejectsInvalidStrategyOutputAndLimit() {
        assertThatThrownBy(() -> filter.filter(null, 10L, 5))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("candidates");
        assertThatThrownBy(() -> filter.filter(List.of(), 10L, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    private RecommendationCandidate candidate(Long productId, double score) {
        return new RecommendationCandidate(productId, score, "test");
    }
}
