package com.training.marketplace.repository;

import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.repository.projection.BestSellerAggregateProjection;
import com.training.marketplace.repository.projection.ProductCoOccurrenceProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o JOIN ProductVariant pv ON oi.variantId = pv.id " +
           "WHERE o.user.id = :userId AND pv.productId = :productId AND o.status != com.training.marketplace.enums.OrderStatus.CANCELLED")
    List<OrderItem> findEligibleOrderItemsForReview(@Param("userId") Long userId, @Param("productId") Long productId);

    @Query("""
            SELECT pv.productId AS productId,
                   COUNT(DISTINCT o.id) AS orderCount,
                   SUM(oi.quantity) AS unitsSold
              FROM OrderItem oi
              JOIN oi.order o
              JOIN ProductVariant pv ON pv.id = oi.variantId
              JOIN Product p ON p.id = pv.productId
             WHERE o.status IN :validStatuses
               AND o.createdAt >= :since
               AND p.active = true
               AND pv.active = true
               AND (:categoryId IS NULL OR p.categoryId = :categoryId)
             GROUP BY pv.productId
             ORDER BY COUNT(DISTINCT o.id) DESC,
                      SUM(oi.quantity) DESC,
                      pv.productId ASC
            """)
    List<BestSellerAggregateProjection> findBestSellingProducts(
            @Param("validStatuses") Collection<OrderStatus> validStatuses,
            @Param("since") LocalDateTime since,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    @Query("""
            SELECT candidateVariant.productId AS productId,
                   COUNT(DISTINCT o.id) AS coOccurrenceCount
              FROM OrderItem sourceItem
              JOIN sourceItem.order o
              JOIN ProductVariant sourceVariant ON sourceVariant.id = sourceItem.variantId
              JOIN o.items candidateItem
              JOIN ProductVariant candidateVariant ON candidateVariant.id = candidateItem.variantId
              JOIN Product candidateProduct ON candidateProduct.id = candidateVariant.productId
             WHERE sourceVariant.productId = :sourceProductId
               AND candidateVariant.productId <> :sourceProductId
               AND o.status IN :validStatuses
               AND o.createdAt >= :since
               AND candidateProduct.active = true
               AND EXISTS (
                   SELECT activeCandidate.id
                     FROM ProductVariant activeCandidate
                    WHERE activeCandidate.productId = candidateVariant.productId
                      AND activeCandidate.active = true
               )
             GROUP BY candidateVariant.productId
            HAVING COUNT(DISTINCT o.id) >= :minOccurrences
             ORDER BY COUNT(DISTINCT o.id) DESC,
                      candidateVariant.productId ASC
            """)
    List<ProductCoOccurrenceProjection> findCoPurchasedProducts(
            @Param("sourceProductId") Long sourceProductId,
            @Param("validStatuses") Collection<OrderStatus> validStatuses,
            @Param("since") LocalDateTime since,
            @Param("minOccurrences") int minOccurrences,
            Pageable pageable);
}
