package com.training.marketplace.service.impl;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.dto.response.ProductPerformanceResponse;
import com.training.marketplace.dto.response.PromotionRecommendationPerformanceResponse;
import com.training.marketplace.dto.response.PromotionTrendPointResponse;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.AnalyticsDashboardQueryRepository;
import com.training.marketplace.service.AnalyticsDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

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

    @Override
    public PageResponse<ProductPerformanceResponse> productPerformance(
            AnalyticsDashboardFilter filter, int page, int size) {
        return productPerformance(filter, page, size, null, "productViews", "desc");
    }

    @Override
    public PageResponse<ProductPerformanceResponse> productPerformance(
            AnalyticsDashboardFilter filter, int page, int size,
            String search, String sortBy, String sortDirection) {
        if (page < 0) {
            throw new BadRequestException("Analytics page must not be negative");
        }
        if (size < 1 || size > 100) {
            throw new BadRequestException("Analytics page size must be between 1 and 100");
        }
        return analyticsDashboardQueryRepository.productPerformance(
                normalize(filter), page, size, search, sortBy, sortDirection);
    }

    @Override
    public List<PromotionRecommendationPerformanceResponse> promotionRecommendationPerformance(
            AnalyticsDashboardFilter filter) {
        return analyticsDashboardQueryRepository.promotionRecommendationPerformance(normalize(filter))
                .stream()
                .map(counts -> new PromotionRecommendationPerformanceResponse(
                        counts.campaign(),
                        counts.placement(),
                        counts.strategy(),
                        counts.impressions(),
                        counts.clicks(),
                        AnalyticsDashboardQueryRepository.rate(counts.clicks(), counts.impressions()),
                        counts.addToCarts(),
                        counts.attributedOrders(),
                        counts.lastUpdatedAt()))
                .toList();
    }

    @Override
    public List<PromotionTrendPointResponse> promotionTrend(AnalyticsDashboardFilter filter) {
        return analyticsDashboardQueryRepository.promotionTrend(normalize(filter));
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
