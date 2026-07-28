package com.training.marketplace.enums;

/**
 * Outcome of validating a coupon against a cart. {@code OK} means valid; every other value is a
 * specific reason the coupon was rejected, surfaced to the storefront so the shopper sees why.
 */
public enum PromotionReason {
    OK,
    EMPTY_CODE,
    CODE_NOT_FOUND,
    INACTIVE,
    NOT_STARTED,
    EXPIRED,
    MIN_ORDER_NOT_MET,
    NO_ELIGIBLE_ITEMS,
    USAGE_LIMIT_REACHED,
    PER_USER_LIMIT_REACHED
}
