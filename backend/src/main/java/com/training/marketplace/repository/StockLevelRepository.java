package com.training.marketplace.repository;

import com.training.marketplace.entity.StockLevel;
import com.training.marketplace.repository.projection.StockLevelProjection;
import com.training.marketplace.repository.projection.StockThresholdProjection;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockLevelRepository extends JpaRepository<StockLevel, Long> {

    Optional<StockLevel> findByWarehouseIdAndVariantId(Long warehouseId, Long variantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sl
            FROM StockLevel sl
            WHERE sl.warehouseId IN :warehouseIds
              AND sl.variantId IN :variantIds
            ORDER BY sl.variantId ASC, sl.warehouseId ASC
            """)
    List<StockLevel> findAllForUpdate(
            @Param("warehouseIds") List<Long> warehouseIds,
            @Param("variantIds") List<Long> variantIds);

    @Query("""
            SELECT
                sl.variantId AS variantId,
                sl.warehouseId AS warehouseId,
                sl.quantity AS quantity,
                v.reorderPoint AS reorderPoint
            FROM StockLevel sl
            JOIN ProductVariant v ON v.id = sl.variantId
            WHERE sl.warehouseId IN :warehouseIds
              AND sl.variantId IN :variantIds
            ORDER BY sl.variantId ASC, sl.warehouseId ASC
            """)
    List<StockThresholdProjection> findAffectedWithThresholds(
            @Param("warehouseIds") List<Long> warehouseIds,
            @Param("variantIds") List<Long> variantIds);

    @Query(
            value = """
                    SELECT
                        sl.id AS id,
                        sl.variantId AS variantId,
                        v.sku AS sku,
                        v.variantName AS variantName,
                        sl.warehouseId AS warehouseId,
                        w.code AS warehouseCode,
                        w.name AS warehouseName,
                        sl.quantity AS quantity,
                        sl.reservedQuantity AS reservedQuantity,
                        (sl.quantity - sl.reservedQuantity) AS availableQuantity,
                        v.minStock AS minStock,
                        v.maxStock AS maxStock,
                        v.reorderPoint AS reorderPoint,
                        sl.version AS version,
                        sl.updatedAt AS updatedAt
                    FROM StockLevel sl
                    JOIN ProductVariant v ON v.id = sl.variantId
                    JOIN Warehouse w ON w.id = sl.warehouseId
                    WHERE (:warehouseId IS NULL OR sl.warehouseId = :warehouseId)
                      AND (:variantId IS NULL OR sl.variantId = :variantId)
                    """,
            countQuery = """
                    SELECT COUNT(sl)
                    FROM StockLevel sl
                    WHERE (:warehouseId IS NULL OR sl.warehouseId = :warehouseId)
                      AND (:variantId IS NULL OR sl.variantId = :variantId)
                    """)
    Page<StockLevelProjection> findAllWithDisplayData(
            @Param("warehouseId") Long warehouseId,
            @Param("variantId") Long variantId,
            Pageable pageable);

    @Query(
            value = """
                    SELECT
                        sl.id AS id,
                        sl.variantId AS variantId,
                        v.sku AS sku,
                        v.variantName AS variantName,
                        sl.warehouseId AS warehouseId,
                        w.code AS warehouseCode,
                        w.name AS warehouseName,
                        sl.quantity AS quantity,
                        sl.reservedQuantity AS reservedQuantity,
                        (sl.quantity - sl.reservedQuantity) AS availableQuantity,
                        v.minStock AS minStock,
                        v.maxStock AS maxStock,
                        v.reorderPoint AS reorderPoint,
                        sl.version AS version,
                        sl.updatedAt AS updatedAt
                    FROM StockLevel sl
                    JOIN ProductVariant v ON v.id = sl.variantId
                    JOIN Warehouse w ON w.id = sl.warehouseId
                    WHERE (:warehouseId IS NULL OR sl.warehouseId = :warehouseId)
                      AND sl.quantity <= v.reorderPoint
                    """,
            countQuery = """
                    SELECT COUNT(sl)
                    FROM StockLevel sl
                    JOIN ProductVariant v ON v.id = sl.variantId
                    WHERE (:warehouseId IS NULL OR sl.warehouseId = :warehouseId)
                      AND sl.quantity <= v.reorderPoint
                    """)
    Page<StockLevelProjection> findLowStockWithDisplayData(
            @Param("warehouseId") Long warehouseId,
            Pageable pageable);
}
