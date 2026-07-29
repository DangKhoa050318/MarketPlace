package com.training.marketplace.service;

import com.training.marketplace.analytics.RecommendationStrategyType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationStrategyRegistryTest {

    @Test
    void resolve_returnsImplementationRegisteredForType() {
        RecommendationStrategy similar = new TestStrategy(RecommendationStrategyType.SIMILAR);
        RecommendationStrategy bestSeller =
                new TestStrategy(RecommendationStrategyType.BEST_SELLER);
        RecommendationCandidateFilter filter = passthroughFilter();
        RecommendationStrategyRegistry registry =
                new RecommendationStrategyRegistry(List.of(similar, bestSeller), filter);

        assertThat(registry.resolve(RecommendationStrategyType.SIMILAR).getType())
                .isEqualTo(RecommendationStrategyType.SIMILAR);
        assertThat(registry.resolve(RecommendationStrategyType.BEST_SELLER).getType())
                .isEqualTo(RecommendationStrategyType.BEST_SELLER);
        assertThat(registry.supports(RecommendationStrategyType.CO_VIEWED)).isFalse();
        assertThat(registry.registeredTypes())
                .containsExactlyInAnyOrder(
                        RecommendationStrategyType.SIMILAR,
                        RecommendationStrategyType.BEST_SELLER);
    }

    @Test
    void constructor_rejectsDuplicateStrategyType() {
        RecommendationStrategy first = new TestStrategy(RecommendationStrategyType.SIMILAR);
        RecommendationStrategy second = new TestStrategy(RecommendationStrategyType.SIMILAR);

        assertThatThrownBy(() -> new RecommendationStrategyRegistry(
                List.of(first, second),
                passthroughFilter()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Multiple recommendation strategies")
                .hasMessageContaining("SIMILAR");
    }

    @Test
    void resolve_rejectsUnregisteredStrategyType() {
        RecommendationStrategyRegistry registry =
                new RecommendationStrategyRegistry(List.of(), passthroughFilter());

        assertThatThrownBy(() -> registry.resolve(RecommendationStrategyType.CO_PURCHASED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No recommendation strategy registered")
                .hasMessageContaining("CO_PURCHASED");
    }

    @Test
    void context_rejectsInvalidIdentifiersAndLimit() {
        assertThatThrownBy(() -> new RecommendationContext(null, null, 0L, null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product ID");
        assertThatThrownBy(() -> new RecommendationContext(null, null, 10L, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void candidate_rejectsInvalidProductAndNonFiniteScore() {
        assertThatThrownBy(() -> new RecommendationCandidate(null, 1.0, "similar"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Product ID");
        assertThatThrownBy(() -> new RecommendationCandidate(10L, Double.NaN, "similar"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("finite");
    }

    @Test
    void strategyContract_returnsRankedInternalCandidates() {
        RecommendationCandidateFilter filter = passthroughFilter();
        RecommendationStrategyRegistry registry = new RecommendationStrategyRegistry(
                List.of(new TestStrategy(RecommendationStrategyType.SIMILAR)),
                filter);
        RecommendationStrategy strategy = registry.resolve(RecommendationStrategyType.SIMILAR);
        RecommendationContext context =
                new RecommendationContext(7L, "session-1", 10L, 2L, 5);

        List<RecommendationCandidate> candidates = strategy.recommend(context);

        assertThat(candidates)
                .extracting(RecommendationCandidate::productId)
                .containsExactly(101L, 102L);
        assertThat(candidates.get(0).score()).isGreaterThan(candidates.get(1).score());
        verify(filter).filter(candidates, 10L, 5);
    }

    private RecommendationCandidateFilter passthroughFilter() {
        RecommendationCandidateFilter filter = mock(RecommendationCandidateFilter.class);
        when(filter.filter(anyList(), any(), anyInt()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        return filter;
    }

    private static final class TestStrategy implements RecommendationStrategy {

        private final RecommendationStrategyType type;

        private TestStrategy(RecommendationStrategyType type) {
            this.type = type;
        }

        @Override
        public RecommendationStrategyType getType() {
            return type;
        }

        @Override
        public List<RecommendationCandidate> recommend(RecommendationContext context) {
            return List.of(
                    new RecommendationCandidate(101L, 0.9, "first"),
                    new RecommendationCandidate(102L, 0.7, "second"));
        }
    }
}
