package com.training.marketplace.repository;

import com.training.marketplace.entity.ProductAnswer;
import com.training.marketplace.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProductAnswerRepository extends JpaRepository<ProductAnswer, Long> {

    List<ProductAnswer> findByQuestionIdAndStatusOrderByIsOfficialDescCreatedAtAsc(Long questionId, ModerationStatus status);

    List<ProductAnswer> findByQuestionIdOrderByIsOfficialDescCreatedAtAsc(Long questionId);

    List<ProductAnswer> findByQuestionIdInAndStatusOrderByIsOfficialDescCreatedAtAsc(List<Long> questionIds, ModerationStatus status);

    @Query("""
        SELECT a FROM ProductAnswer a
        WHERE (:status IS NULL OR a.status = :status)
          AND (:productId IS NULL OR a.question.product.id = :productId)
          AND (:startDate IS NULL OR a.createdAt >= :startDate)
          AND (:endDate IS NULL OR a.createdAt <= :endDate)
    """)
    Page<ProductAnswer> findForModeration(
            @Param("status") ModerationStatus status,
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
