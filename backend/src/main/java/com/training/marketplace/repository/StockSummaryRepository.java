package com.training.marketplace.repository;

import com.training.marketplace.entity.StockLevel;
import com.training.marketplace.repository.projection.StockSummaryProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface StockSummaryRepository extends Repository<StockLevel, Long> {

    @Query(
            value = """
                    SELECT
                        variant_id AS "variantId",
                        sku AS "sku",
                        variant_name AS "variantName",
                        product_name AS "productName",
                        category_name AS "categoryName",
                        warehouse_id AS "warehouseId",
                        warehouse_name AS "warehouseName",
                        quantity AS "quantity",
                        reserved_quantity AS "reservedQuantity",
                        available_quantity AS "availableQuantity",
                        min_stock AS "minStock",
                        reorder_point AS "reorderPoint",
                        stock_status AS "stockStatus"
                    FROM mv_stock_summary
                    WHERE (:warehouseId IS NULL OR warehouse_id = :warehouseId)
                      AND (:variantId IS NULL OR variant_id = :variantId)
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM mv_stock_summary
                    WHERE (:warehouseId IS NULL OR warehouse_id = :warehouseId)
                      AND (:variantId IS NULL OR variant_id = :variantId)
                    """,
            nativeQuery = true)
    Page<StockSummaryProjection> findAllWithFilters(
            @Param("warehouseId") Long warehouseId,
            @Param("variantId") Long variantId,
            Pageable pageable);
}
