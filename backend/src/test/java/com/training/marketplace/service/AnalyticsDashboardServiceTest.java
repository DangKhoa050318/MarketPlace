package com.training.marketplace.service;

import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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