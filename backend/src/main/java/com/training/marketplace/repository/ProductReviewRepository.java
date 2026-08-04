package com.training.marketplace.repository;

import com.training.marketplace.entity.ProductReview;
import com.training.marketplace.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long>, JpaSpecificationExecutor<ProductReview> {

    Optional<ProductReview> findByUserIdAndProductIdAndDeletedAtIsNull(Long userId, Long productId);

    boolean existsByUserIdAndProductIdAndDeletedAtIsNull(Long userId, Long productId);

    Page<ProductReview> findByProductIdAndStatusAndDeletedAtIsNull(Long productId, ReviewStatus status, Pageable pageable);

    Page<ProductReview> findByProductIdAndStatusAndRatingAndDeletedAtIsNull(Long productId, ReviewStatus status, Integer rating, Pageable pageable);

    @Query("SELECT r.rating AS rating, COUNT(r) AS count FROM ProductReview r " +
           "WHERE r.product.id = :productId AND r.status = com.training.marketplace.enums.ReviewStatus.APPROVED " +
           "AND r.deletedAt IS NULL GROUP BY r.rating")
    List<Object[]> countReviewsGroupByRating(@Param("productId") Long productId);

    // G1: batch average rating + review count per product (approved, non-deleted) for storefront cards.
    @Query("SELECT r.product.id AS productId, AVG(r.rating) AS avg, COUNT(r) AS cnt FROM ProductReview r " +
           "WHERE r.product.id IN :productIds AND r.status = com.training.marketplace.enums.ReviewStatus.APPROVED " +
           "AND r.deletedAt IS NULL GROUP BY r.product.id")
    List<Object[]> aggregateRatingsByProductIds(@Param("productIds") List<Long> productIds);

    Page<ProductReview> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    boolean existsByUserIdAndOrderItemIdAndDeletedAtIsNull(Long userId, Long orderItemId);

    @Query("SELECT DISTINCT r.product.id FROM ProductReview r WHERE r.user.id = :userId AND r.deletedAt IS NULL")
    List<Long> findReviewedProductIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT DISTINCT r.orderItem.id FROM ProductReview r WHERE r.user.id = :userId AND r.orderItem.id IS NOT NULL AND r.deletedAt IS NULL")
    List<Long> findReviewedOrderItemIdsByUserId(@Param("userId") Long userId);
}
