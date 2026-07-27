package com.training.marketplace.service;

import com.training.marketplace.dto.response.RecentlyViewedProductResponse;

import java.util.List;

public interface RecentlyViewedProductService {

    RecentlyViewedProductResponse record(Long userId, String sessionId, Long productId);

    List<RecentlyViewedProductResponse> list(Long userId, String sessionId, int limit);

    void clear(Long userId, String sessionId);
}
