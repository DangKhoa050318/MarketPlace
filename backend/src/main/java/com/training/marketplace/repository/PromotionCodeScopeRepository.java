package com.training.marketplace.repository;

import com.training.marketplace.entity.PromotionCodeScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PromotionCodeScopeRepository extends JpaRepository<PromotionCodeScope, Long> {

    List<PromotionCodeScope> findByPromotionCodeId(Long promotionCodeId);

    void deleteByPromotionCodeId(Long promotionCodeId);
}
