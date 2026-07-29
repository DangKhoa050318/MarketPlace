package com.training.marketplace.repository;

import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.repository.projection.ProductPriceRangeProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductId(Long productId);
    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);
    List<ProductVariant> findByProductIdInAndActiveTrue(Collection<Long> productIds);
    Optional<ProductVariant> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsByProductIdAndVariantName(Long productId, String variantName);

    @Query("""
            SELECT v.productId AS productId,
                   MIN(v.price) AS minPrice,
                   MAX(v.price) AS maxPrice
              FROM ProductVariant v
             WHERE v.active = true
               AND v.productId IN :productIds
             GROUP BY v.productId
            """)
    List<ProductPriceRangeProjection> findActivePriceRanges(
            @Param("productIds") Collection<Long> productIds);
}
