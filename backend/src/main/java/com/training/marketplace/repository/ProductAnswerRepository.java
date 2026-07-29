package com.training.marketplace.repository;

import com.training.marketplace.entity.ProductAnswer;
import com.training.marketplace.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductAnswerRepository extends JpaRepository<ProductAnswer, Long>, JpaSpecificationExecutor<ProductAnswer> {

    @Query("SELECT a FROM ProductAnswer a WHERE a.question.id = :questionId AND a.status = :status ORDER BY a.isOfficial DESC, a.createdAt ASC")
    List<ProductAnswer> findByQuestionIdAndStatusOrderByIsOfficialDescCreatedAtAsc(@Param("questionId") Long questionId, @Param("status") ModerationStatus status);

    @Query("SELECT a FROM ProductAnswer a WHERE a.question.id = :questionId ORDER BY a.isOfficial DESC, a.createdAt ASC")
    List<ProductAnswer> findByQuestionIdOrderByIsOfficialDescCreatedAtAsc(@Param("questionId") Long questionId);

    @Query("SELECT a FROM ProductAnswer a WHERE a.question.id IN :questionIds AND a.status = :status ORDER BY a.isOfficial DESC, a.createdAt ASC")
    List<ProductAnswer> findByQuestionIdInAndStatusOrderByIsOfficialDescCreatedAtAsc(@Param("questionIds") List<Long> questionIds, @Param("status") ModerationStatus status);
}
