package com.training.marketplace.dto.response;

import com.training.marketplace.enums.DiscountType;

import java.math.BigDecimal;

/**
 * Result of previewing a coupon against the current cart. For a business-invalid coupon this is
 * still returned with HTTP 200, {@code valid=false} and a {@code reason} the storefront can show.
 */
public record CouponPreviewResponse(
        String code,
        boolean valid,
        String reason,              // null when valid; otherwise a PromotionReason name
        DiscountType discountType,  // null when invalid / code not found
        BigDecimal eligibleSubtotal,
        BigDecimal discountAmount,
        BigDecimal cartSubtotal,
        BigDecimal newTotal
) {}
