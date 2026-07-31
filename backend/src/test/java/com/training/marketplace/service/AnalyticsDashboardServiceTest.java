package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.dto.response.ProductPerformanceResponse;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.AnalyticsDashboardQueryRepository;
import com.training.marketplace.service.impl.AnalyticsDashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalyticsDashboardServiceTest {

    @Mock private AnalyticsDashboardQueryRepository analyticsDashboardQueryRepository;

    @InjectMocks private AnalyticsDashboardServiceImpl analyticsDashboardService;

    @Test
    void overview_calculatesRatesFromCounts() {
        when(analyticsDashboardQueryRepository.overview(any()))
                .thenReturn(new AnalyticsOverviewResponse(
                        100,
                        25,
                        10,
                        5,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("0.3333"),
                        Instant.parse("2026-07-30T00:00:00Z")));

        var response = analyticsDashboardService.overview(filter());

        assertThat(response.addToCartRate()).isEqualByComparingTo("0.2500");
        assertThat(response.checkoutRate()).isEqualByComparingTo("0.4000");
        assertThat(response.orderConversionRate()).isEqualByComparingTo("0.0500");
        assertThat(response.returningCustomerRate()).isEqualByComparingTo("0.3333");
        assertThat(response.lastUpdatedAt()).isEqualTo(Instant.parse("2026-07-30T00:00:00Z"));
    }

    @Test
    void overview_rejectsInvalidDateRange() {
        AnalyticsDashboardFilter invalid = new AnalyticsDashboardFilter(
                Instant.parse("2026-08-01T00:00:00Z"),
                Instant.parse("2026-07-01T00:00:00Z"),
                null,
                null,
                null,
                null,
                null);

        assertThatThrownBy(() -> analyticsDashboardService.overview(invalid))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("from must be before to");
    }

    @Test
    void productPerformance_normalizesFilterAndReturnsPage() {
        var expected = new PageResponse<ProductPerformanceResponse>(List.of(), 1, 25, 0, 0, true);
        when(analyticsDashboardQueryRepository.productPerformance(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(expected);

        var response = analyticsDashboardService.productPerformance(filter(), 1, 25);

        assertThat(response).isSameAs(expected);
        verify(analyticsDashboardQueryRepository).productPerformance(
                new AnalyticsDashboardFilter(
                        filter().from(), filter().to(), null, null,
                        "summer", "HOME_BEST_SELLERS", "mobile"),
                1,
                25);
    }

    @Test
    void productPerformance_rejectsOversizedPage() {
        assertThatThrownBy(() -> analyticsDashboardService.productPerformance(filter(), 0, 101))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("between 1 and 100");
    }

    private AnalyticsDashboardFilter filter() {
        return new AnalyticsDashboardFilter(
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-08-01T00:00:00Z"),
                null,
                null,
                " summer ",
                "HOME_BEST_SELLERS",
                "mobile");
    }
}
