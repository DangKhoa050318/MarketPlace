package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.config.RecommendationProperties;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.repository.projection.ProductCoOccurrenceProjection;
import com.training.marketplace.service.RecommendationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoViewedRecommendationStrategyTest {

    private static final Instant NOW = Instant.parse("2026-07-29T00:00:00Z");

    @Mock
    private AnalyticsEventRepository analyticsEventRepository;

    @Mock
    private RecommendationSourceProductValidator sourceProductValidator;

    private RecommendationProperties properties;
    private CoViewedRecommendationStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new RecommendationProperties();
        strategy = new CoViewedRecommendationStrategy(
                analyticsEventRepository,
                sourceProductValidator,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void recommend_usesConfiguredWindowThresholdAndLimit() {
        when(analyticsEventRepository.findCoViewedProducts(
                eq(10L), any(Instant.class), eq(2), any(Pageable.class)))
                .thenReturn(List.of(
                        occurrence(20L, 8L),
                        occurrence(30L, 5L)));
        RecommendationContext context =
                new RecommendationContext(null, "session-1", 10L, null, 2);

        var result = strategy.recommend(context);

        assertThat(strategy.getType()).isEqualTo(RecommendationStrategyType.CO_VIEWED);
        assertThat(result)
                .extracting(candidate -> candidate.productId())
                .containsExactly(20L, 30L);
        assertThat(result.get(0).score()).isEqualTo(8.0);
        assertThat(result.get(0).reason())
                .isEqualTo("co_viewed_actors=8,window=PT2160H");

        ArgumentCaptor<Instant> sinceCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(analyticsEventRepository).findCoViewedProducts(
                eq(10L), sinceCaptor.capture(), eq(2), pageableCaptor.capture());
        assertThat(sinceCaptor.getValue()).isEqualTo(NOW.minus(Duration.ofDays(90)));
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
        verify(sourceProductValidator).requireActive(10L, RecommendationStrategyType.CO_VIEWED);
    }

    @Test
    void recommend_honorsCustomLookbackAndThreshold() {
        properties.setCoViewedLookback(Duration.ofDays(14));
        properties.setMinCoViewedOccurrences(4);
        when(analyticsEventRepository.findCoViewedProducts(
                eq(10L), any(Instant.class), eq(4), any(Pageable.class)))
                .thenReturn(List.of());

        strategy.recommend(new RecommendationContext(null, null, 10L, null, 5));

        verify(analyticsEventRepository).findCoViewedProducts(
                eq(10L),
                eq(NOW.minus(Duration.ofDays(14))),
                eq(4),
                PageRequestMatcher.pageSize(5));
    }

    @Test
    void recommend_returnsEmptyWhenThresholdHasNoMatches() {
        when(analyticsEventRepository.findCoViewedProducts(
                eq(10L), any(Instant.class), eq(2), any(Pageable.class)))
                .thenReturn(List.of());

        assertThat(strategy.recommend(
                new RecommendationContext(null, "session-1", 10L, null, 10)))
                .isEmpty();
    }

    @Test
    void recommend_rejectsOccurrenceThresholdBelowTwo() {
        properties.setMinCoViewedOccurrences(1);

        assertThatThrownBy(() -> strategy.recommend(
                new RecommendationContext(null, null, 10L, null, 10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 2");
        verifyNoInteractions(analyticsEventRepository);
    }

    private ProductCoOccurrenceProjection occurrence(Long productId, Long count) {
        return new TestOccurrence(productId, count);
    }
}
