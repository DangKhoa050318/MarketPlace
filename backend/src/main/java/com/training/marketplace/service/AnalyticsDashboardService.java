package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.dto.response.ProductPerformanceResponse;
import com.training.marketplace.dto.response.PromotionRecommendationPerformanceResponse;
import com.training.marketplace.dto.response.PromotionTrendPointResponse;

import java.util.List;

public interface AnalyticsDashboardService {

    AnalyticsOverviewResponse overview(AnalyticsDashboardFilter filter);

    PageResponse<ProductPerformanceResponse> productPerformance(
            AnalyticsDashboardFilter filter, int page, int size);

    PageResponse<ProductPerformanceResponse> productPerformance(
            AnalyticsDashboardFilter filter, int page, int size,
            String search, String sortBy, String sortDirection);

    List<PromotionRecommendationPerformanceResponse> promotionRecommendationPerformance(
            AnalyticsDashboardFilter filter);

    List<PromotionTrendPointResponse> promotionTrend(AnalyticsDashboardFilter filter);
}
