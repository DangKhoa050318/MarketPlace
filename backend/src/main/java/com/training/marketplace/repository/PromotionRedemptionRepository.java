package com.training.marketplace.repository;

import com.training.marketplace.entity.PromotionRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromotionRedemptionRepository extends JpaRepository<PromotionRedemption, Long> {

    long countByPromotionCodeIdAndUserId(Long promotionCodeId, Long userId);

    boolean existsByOrderId(Long orderId);

    void deleteByOrderId(Long orderId);
}
