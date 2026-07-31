package com.training.marketplace.repository;

import com.training.marketplace.entity.ProductQuestion;
import com.training.marketplace.enums.ModerationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long>, JpaSpecificationExecutor<ProductQuestion> {

    Page<ProductQuestion> findByProductIdAndStatus(Long productId, ModerationStatus status, Pageable pageable);

    Page<ProductQuestion> findByProductId(Long productId, Pageable pageable);
}
