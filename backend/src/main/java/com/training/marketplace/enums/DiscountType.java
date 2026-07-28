package com.training.marketplace.enums;

/**
 * How a {@link com.training.marketplace.entity.PromotionCode} reduces the eligible subtotal.
 * PERCENT — a percentage (1..100), optionally capped by {@code maxDiscount}.
 * FIXED   — a fixed money amount, clamped so the total never goes negative.
 */
public enum DiscountType {
    PERCENT,
    FIXED
}
