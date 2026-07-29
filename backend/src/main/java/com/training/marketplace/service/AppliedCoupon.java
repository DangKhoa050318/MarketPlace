package com.training.marketplace.service;

import java.math.BigDecimal;

/**
 * Outcome of consuming a coupon at order time: which coupon was applied and how much it took off.
 * Returned by {@code PromotionService.consume} after the coupon row has been locked and its
 * {@code usedCount} incremented.
 */
public record AppliedCoupon(
        Long promotionCodeId,
        String code,
        BigDecimal discountAmount
) {}
