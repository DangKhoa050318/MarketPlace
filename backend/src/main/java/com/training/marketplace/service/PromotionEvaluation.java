package com.training.marketplace.service;

import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.enums.PromotionReason;

import java.math.BigDecimal;

/**
 * Result of evaluating a coupon against a cart. {@code valid} + {@code reason == OK} means the
 * coupon applies and {@code discountAmount}/{@code newTotal} are meaningful; otherwise {@code reason}
 * says why it was rejected and discount is zero. {@code promo} is null when the code was not found.
 */
public record PromotionEvaluation(
        boolean valid,
        PromotionReason reason,
        PromotionCode promo,
        BigDecimal eligibleSubtotal,
        BigDecimal discountAmount,
        BigDecimal cartSubtotal,
        BigDecimal newTotal
) {
    public static PromotionEvaluation invalid(PromotionReason reason, PromotionCode promo, BigDecimal cartSubtotal) {
        return new PromotionEvaluation(false, reason, promo, BigDecimal.ZERO, BigDecimal.ZERO, cartSubtotal, cartSubtotal);
    }

    public static PromotionEvaluation ok(PromotionCode promo, BigDecimal eligibleSubtotal,
                                         BigDecimal discountAmount, BigDecimal cartSubtotal) {
        return new PromotionEvaluation(true, PromotionReason.OK, promo, eligibleSubtotal, discountAmount,
                cartSubtotal, cartSubtotal.subtract(discountAmount));
    }
}
