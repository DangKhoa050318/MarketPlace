package com.training.marketplace.service;

import com.training.marketplace.dto.response.FunnelSummaryResponse;

import java.time.Instant;

public interface FunnelAnalyticsService {

    FunnelSummaryResponse summarize(
            Instant from,
            Instant to,
            Long categoryId,
            Long productId,
            String campaign,
            String deviceType);
}
