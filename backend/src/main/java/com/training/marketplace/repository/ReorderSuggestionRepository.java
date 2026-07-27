package com.training.marketplace.repository;

import com.training.marketplace.entity.ReorderSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReorderSuggestionRepository
        extends JpaRepository<ReorderSuggestion, Long> {

    @Modifying
    @Query(
            value = """
                    INSERT INTO reorder_suggestions (
                        variant_id,
                        warehouse_id,
                        suggested_quantity,
                        current_stock,
                        reorder_point,
                        status,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        :variantId,
                        :warehouseId,
                        :suggestedQuantity,
                        :currentStock,
                        :reorderPoint,
                        'PENDING',
                        NOW(),
                        NOW()
                    )
                    ON CONFLICT (variant_id, warehouse_id)
                        WHERE status = 'PENDING'
                    DO NOTHING
                    """,
            nativeQuery = true)
    int insertPendingIfAbsent(
            @Param("variantId") Long variantId,
            @Param("warehouseId") Long warehouseId,
            @Param("suggestedQuantity") Integer suggestedQuantity,
            @Param("currentStock") Integer currentStock,
            @Param("reorderPoint") Integer reorderPoint);
}
