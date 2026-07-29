package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.config.RecommendationProperties;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.repository.OrderItemRepository;
import com.training.marketplace.repository.projection.BestSellerAggregateProjection;
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
import java.util.EnumSet;
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
class BestSellerRecommendationStrategyTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private static final Instant NOW = Instant.parse("2026-07-29T00:00:00Z");

    @Mock
    private OrderItemRepository orderItemRepository;

    private RecommendationProperties properties;
    private BestSellerRecommendationStrategy strategy;

    @BeforeEach
    void setUp() {
        properties = new RecommendationProperties();
        strategy = new BestSellerRecommendationStrategy(
                orderItemRepository,
                properties,
                Clock.fixed(NOW, ZONE));
    }

    @Test
    void getType_returnsBestSeller() {
        assertThat(strategy.getType()).isEqualTo(RecommendationStrategyType.BEST_SELLER);
    }

    @Test
    void recommend_usesValidStatusesWindowCategoryAndLimit() {
        when(orderItemRepository.findBestSellingProducts(
                anyCollection(), any(LocalDateTime.class), eq(4L), any(Pageable.class)))
                .thenReturn(List.of(
                        aggregate(20L, 12L, 30L),
                        aggregate(30L, 8L, 25L)));
        RecommendationContext context =
                new RecommendationContext(null, "session-1", null, 4L, 2);

        var result = strategy.recommend(context);

        assertThat(result)
                .extracting(candidate -> candidate.productId())
                .containsExactly(20L, 30L);
        assertThat(result.get(0).score()).isEqualTo(12.0);
        assertThat(result.get(0).reason())
                .isEqualTo("valid_orders=12,units_sold=30,window=PT720H");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<OrderStatus>> statusesCaptor =
                ArgumentCaptor.forClass(Collection.class);
        ArgumentCaptor<LocalDateTime> sinceCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderItemRepository).findBestSellingProducts(
                statusesCaptor.capture(),
                sinceCaptor.capture(),
                eq(4L),
                pageableCaptor.capture());

        assertThat(statusesCaptor.getValue())
                .containsExactlyInAnyOrder(
                        OrderStatus.CONFIRMED,
                        OrderStatus.PROCESSING,
                        OrderStatus.SHIPPED,
                        OrderStatus.DELIVERED)
                .doesNotContain(OrderStatus.PENDING, OrderStatus.CANCELLED);
        assertThat(sinceCaptor.getValue())
                .isEqualTo(LocalDateTime.of(2026, 6, 29, 7, 0));
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    void recommend_honorsCustomLookbackAndStatusConfiguration() {
        properties.setBestSellerLookback(Duration.ofDays(7));
        properties.setValidOrderStatuses(EnumSet.of(OrderStatus.DELIVERED));
        when(orderItemRepository.findBestSellingProducts(
                anyCollection(), any(LocalDateTime.class), eq(null), any(Pageable.class)))
                .thenReturn(List.of());

        strategy.recommend(new RecommendationContext(null, null, null, null, 5));

        ArgumentCaptor<LocalDateTime> sinceCaptor =
                ArgumentCaptor.forClass(LocalDateTime.class);
        verify(orderItemRepository).findBestSellingProducts(
                eq(EnumSet.of(OrderStatus.DELIVERED)),
                sinceCaptor.capture(),
                eq(null),
                any(Pageable.class));
        assertThat(sinceCaptor.getValue())
                .isEqualTo(LocalDateTime.of(2026, 7, 22, 7, 0));
    }

    @Test
    void recommend_returnsEmptyWhenNoValidOrdersExist() {
        when(orderItemRepository.findBestSellingProducts(
                anyCollection(), any(LocalDateTime.class), eq(null), any(Pageable.class)))
                .thenReturn(List.of());

        var result = strategy.recommend(
                new RecommendationContext(null, "session-1", null, null, 10));

        assertThat(result).isEmpty();
    }

    @Test
    void recommend_rejectsPendingOrCancelledAsValidStatuses() {
        properties.setValidOrderStatuses(
                EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED));

        assertThatThrownBy(() -> strategy.recommend(
                new RecommendationContext(null, null, null, null, 10)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exclude PENDING and CANCELLED");
        verifyNoInteractions(orderItemRepository);
    }

    private BestSellerAggregateProjection aggregate(
            Long productId,
            Long orderCount,
            Long unitsSold) {
        return new BestSellerAggregateProjection() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public Long getOrderCount() {
                return orderCount;
            }

            @Override
            public Long getUnitsSold() {
                return unitsSold;
            }
        };
    }
}
