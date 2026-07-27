package com.training.marketplace.service;

import com.training.marketplace.dto.response.StockLevelResponse;

import java.util.Collection;
import java.util.Optional;

public interface StockCacheService {

    Optional<StockLevelResponse> get(Long warehouseId, Long productId);

    void put(Long warehouseId, Long productId, StockLevelResponse response);

    void evictAfterCommit(Collection<StockCacheKey> keys);
}
