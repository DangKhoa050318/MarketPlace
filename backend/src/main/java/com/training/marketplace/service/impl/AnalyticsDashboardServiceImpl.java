package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.AnalyticsDashboardQueryRepository;
import com.training.marketplace.service.AnalyticsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AnalyticsDashboardServiceImpl implements AnalyticsDashboardService {

    private static final Duration MAX_QUERY_RANGE = Duration.ofDays(366);

    private final AnalyticsDashboardQueryRepository analyticsDashboardQueryRepository;

    @Override
    public AnalyticsOverviewResponse overview(AnalyticsDashboardFilter filter) {
        AnalyticsDashboardFilter normalized = normalize(filter);
        AnalyticsOverviewResponse counts = analyticsDashboardQueryRepository.overview(normalized);
        return new AnalyticsOverviewResponse(
                counts.productViews(),
                counts.addToCarts(),
                counts.beginCheckouts(),
                counts.orders(),
                AnalyticsDashboardQueryRepository.rate(counts.addToCarts(), counts.productViews()),
                AnalyticsDashboardQueryRepository.rate(counts.beginCheckouts(), counts.addToCarts()),
                AnalyticsDashboardQueryRepository.rate(counts.orders(), counts.productViews()),
                counts.returningCustomerRate(),
                counts.lastUpdatedAt());
    }

    private AnalyticsDashboardFilter normalize(AnalyticsDashboardFilter filter) {
        if (filter.from() == null || filter.to() == null) {
            throw new BadRequestException("Analytics date range is required");
        }
        if (!filter.from().isBefore(filter.to())) {
            throw new BadRequestException("Analytics from must be before to");
        }
        if (Duration.between(filter.from(), filter.to()).compareTo(MAX_QUERY_RANGE) > 0) {
            throw new BadRequestException("Analytics date range must not exceed 366 days");
        }
        return new AnalyticsDashboardFilter(
                filter.from(),
                filter.to(),
                filter.categoryId(),
                filter.productId(),
                blankToNull(filter.campaign()),
                blankToNull(filter.placement()),
                blankToNull(filter.deviceType()));
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}