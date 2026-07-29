package com.training.marketplace.service;

import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.enums.DiscountType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Computes the money discount for a coupon against the eligible subtotal (REQ-STP-B-306).
 * Pure function, no I/O. All arithmetic uses {@link BigDecimal} with scale 2, HALF_UP, and the
 * result is clamped to {@code [0, eligibleSubtotal]} so the order total can never go negative.
 */
@Service
public class DiscountCalculationService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public BigDecimal calculate(PromotionCode promo, BigDecimal eligibleSubtotal) {
        if (eligibleSubtotal == null || eligibleSubtotal.signum() <= 0) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }

        BigDecimal discount;
        if (promo.getDiscountType() == DiscountType.FIXED) {
            discount = promo.getDiscountValue();
        } else { // PERCENT
            BigDecimal raw = eligibleSubtotal
                    .multiply(promo.getDiscountValue())
                    .divide(HUNDRED, SCALE, ROUNDING);
            discount = (promo.getMaxDiscount() != null) ? raw.min(promo.getMaxDiscount()) : raw;
        }

        // Clamp to [0, eligibleSubtotal] — total never negative.
        if (discount.signum() < 0) {
            discount = BigDecimal.ZERO;
        }
        if (discount.compareTo(eligibleSubtotal) > 0) {
            discount = eligibleSubtotal;
        }
        return discount.setScale(SCALE, ROUNDING);
    }
}
