package com.training.marketplace.service;

import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.dto.response.RecommendationResponse;

public interface RecommendationService {

    RecommendationResponse recommend(
            Long userId,
            String sessionId,
            RecommendationPlacement placement,
            Long productId,
            Long categoryId,
            int limit);
}
