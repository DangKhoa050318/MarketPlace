package com.training.marketplace.repository;

import com.training.marketplace.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);
    boolean existsBySlug(String slug);
    long countByActiveTrue();
    List<Product> findAllByActiveTrue();

    @Query("""
            SELECT p.id
              FROM Product p
             WHERE p.id IN :productIds
               AND p.active = true
               AND EXISTS (
                   SELECT v.id
                     FROM ProductVariant v
                    WHERE v.productId = p.id
                      AND v.active = true
               )
            """)
    List<Long> findRecommendationEligibleProductIds(
            @Param("productIds") Collection<Long> productIds);
    List<Product> findAllByIdInAndActiveTrue(Collection<Long> productIds);

    // Stock locking now happens on stock_levels (see InventoryFacade), not on products.
    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.slug) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);
}
