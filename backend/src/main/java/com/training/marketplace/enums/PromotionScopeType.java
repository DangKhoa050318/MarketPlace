package com.training.marketplace.enums;

/**
 * What part of the cart a coupon applies to.
 * CART     — the whole cart subtotal.
 * PRODUCT  — only items whose product (SPU) is listed in the coupon scope.
 * CATEGORY — only items whose product's category is listed in the coupon scope.
 */
public enum PromotionScopeType {
    CART,
    PRODUCT,
    CATEGORY
}
