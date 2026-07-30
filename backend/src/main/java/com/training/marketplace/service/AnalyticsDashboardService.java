package com.training.marketplace.service;

import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;

public interface AnalyticsDashboardService {

    AnalyticsOverviewResponse overview(AnalyticsDashboardFilter filter);
}