package com.training.marketplace.enums;

/**
 * The kind of entity a {@link com.training.marketplace.entity.PromotionCodeScope} row references.
 * PRODUCT points at {@code products.id} (SPU); CATEGORY points at {@code categories.id}.
 */
public enum ScopeRefType {
    PRODUCT,
    CATEGORY
}
