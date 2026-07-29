package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.config.RecommendationProperties;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.repository.OrderItemRepository;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoPurchasedRecommendationStrategyTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final Instant NOW = Instant.parse("2026-07-29T00:00:00Z");

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private RecommendationSourceProductValidator sourceProductValidator;

    private RecommendationProperties properties;
    private CoPurchasedRecommendationStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new RecommendationProperties();
        strategy = new CoPurchasedRecommendationStrategy(
                orderItemRepository,
                sourceProductValidator,
                properties,
                Clock.fixed(NOW, ZONE));
    }

    @Test
    void recommend_usesDistinctValidOrderAggregationConfiguration() {
        when(orderItemRepository.findCoPurchasedProducts(
                eq(10L), anyCollection(), any(LocalDateTime.class), eq(2), any(Pageable.class)))
                .thenReturn(List.of(
                        new TestOccurrence(20L, 7L),
                        new TestOccurrence(30L, 3L)));
        RecommendationContext context =
                new RecommendationContext(7L, "session-1", 10L, null, 2);

        var result = strategy.recommend(context);

        assertThat(strategy.getType()).isEqualTo(RecommendationStrategyType.CO_PURCHASED);
        assertThat(result)
                .extracting(candidate -> candidate.productId())
                .containsExactly(20L, 30L);
        assertThat(result.get(0).score()).isEqualTo(7.0);
        assertThat(result.get(0).reason())
                .isEqualTo("co_purchased_orders=7,window=PT2160H");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<OrderStatus>> statusesCaptor =
                ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<LocalDateTime> sinceCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderItemRepository).findCoPurchasedProducts(
                eq(10L),
                statusesCaptor.capture(),
                sinceCaptor.capture(),
                eq(2),
                pageableCaptor.capture());
        assertThat(statusesCaptor.getValue())
                .containsExactlyInAnyOrder(
                        OrderStatus.CONFIRMED,
                        OrderStatus.PROCESSING,
                        OrderStatus.SHIPPED,
                        OrderStatus.DELIVERED);
        assertThat(sinceCaptor.getValue())
                .isEqualTo(LocalDateTime.of(2026, 4, 30, 7, 0));
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
        verify(sourceProductValidator)
                .requireActive(10L, RecommendationStrategyType.CO_PURCHASED);
    }

    @Test
    void recommend_honorsCustomLookbackAndThreshold() {
        properties.setCoPurchasedLookback(Duration.ofDays(30));
        properties.setMinCoPurchasedOccurrences(5);
        when(orderItemRepository.findCoPurchasedProducts(
                eq(10L), anyCollection(), any(LocalDateTime.class), eq(5), any(Pageable.class)))
                .thenReturn(List.of());

        strategy.recommend(new RecommendationContext(null, null, 10L, null, 6));

        verify(orderItemRepository).findCoPurchasedProducts(
                eq(10L),
                anyCollection(),
                eq(LocalDateTime.of(2026, 6, 29, 7, 0)),
                eq(5),
                PageRequestMatcher.pageSize(6));
    }

    @Test
    void recommend_returnsEmptyWhenThresholdHasNoMatches() {
        when(orderItemRepository.findCoPurchasedProducts(
                eq(10L), anyCollection(), any(LocalDateTime.class), eq(2), any(Pageable.class)))
                .thenReturn(List.of());

        assertThat(strategy.recommend(
                new RecommendationContext(null, null, 10L, null, 10)))
                .isEmpty();
    }

    @Test
    void recommend_rejectsOccurrenceThresholdBelowTwo() {
        properties.setMinCoPurchasedOccurrences(1);

        assertThatThrownBy(() -> strategy.recommend(
                new RecommendationContext(null, null, 10L, null, 10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 2");
        verifyNoInteractions(orderItemRepository);
    }
}
