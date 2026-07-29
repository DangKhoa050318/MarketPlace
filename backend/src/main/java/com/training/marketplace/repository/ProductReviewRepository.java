package com.training.marketplace.repository;

import com.training.marketplace.entity.ProductReview;
import com.training.marketplace.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    Optional<ProductReview> findByUserIdAndProductIdAndDeletedAtIsNull(Long userId, Long productId);

    boolean existsByUserIdAndProductIdAndDeletedAtIsNull(Long userId, Long productId);

    Page<ProductReview> findByProductIdAndStatusAndDeletedAtIsNull(Long productId, ReviewStatus status, Pageable pageable);

    Page<ProductReview> findByProductIdAndStatusAndRatingAndDeletedAtIsNull(Long productId, ReviewStatus status, Integer rating, Pageable pageable);

    @Query("SELECT r.rating AS rating, COUNT(r) AS count FROM ProductReview r " +
           "WHERE r.product.id = :productId AND r.status = com.training.marketplace.enums.ReviewStatus.APPROVED " +
           "AND r.deletedAt IS NULL GROUP BY r.rating")
    List<Object[]> countReviewsGroupByRating(@Param("productId") Long productId);

    Page<ProductReview> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    @Query("""
        SELECT r FROM ProductReview r
        WHERE (:status IS NULL OR r.status = :status)
          AND (:productId IS NULL OR r.product.id = :productId)
          AND (:startDate IS NULL OR r.createdAt >= :startDate)
          AND (:endDate IS NULL OR r.createdAt <= :endDate)
          AND r.deletedAt IS NULL
    """)
    Page<ProductReview> findForModeration(
            @Param("status") ReviewStatus status,
            @Param("productId") Long productId,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            Pageable pageable
    );
}
