package com.training.marketplace.service;

import com.training.marketplace.dto.response.StockLevelResponse;
import com.training.marketplace.dto.response.StockSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface StockLevelService {

    StockLevelResponse getByWarehouseAndProduct(Long warehouseId, Long productId);

    Page<StockLevelResponse> getAll(Long warehouseId, Long productId, Pageable pageable);

    Page<StockLevelResponse> getLowStock(Long warehouseId, Pageable pageable);

    Page<StockSummaryResponse> getSummary(
            Long warehouseId,
            Long productId,
            Pageable pageable);
}
