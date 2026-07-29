package com.training.marketplace.repository;

import com.training.marketplace.entity.ProductQuestion;
import com.training.marketplace.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {

    Page<ProductQuestion> findByProductIdAndStatus(Long productId, ModerationStatus status, Pageable pageable);

    Page<ProductQuestion> findByProductId(Long productId, Pageable pageable);

    @Query("""
        SELECT q FROM ProductQuestion q
        WHERE (:status IS NULL OR q.status = :status)
          AND (:productId IS NULL OR q.product.id = :productId)
          AND (:startDate IS NULL OR q.createdAt >= :startDate)
          AND (:endDate IS NULL OR q.createdAt <= :endDate)
    """)
    Page<ProductQuestion> findForModeration(
            @Param("status") ModerationStatus status,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
