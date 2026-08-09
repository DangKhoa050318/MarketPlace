package com.training.marketplace.service;

import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

public record ChatCampaignOffer(
        Long campaignId,
        String campaignName,
        String campaignDescription,
        Long promotionCodeId,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscount,
        BigDecimal minOrderAmount,
        PromotionScopeType scopeType,
        LocalDateTime expiresAt,
        Map<ScopeRefType, Set<Long>> scopes
) {
    public ChatCampaignOffer {
        scopes = scopes == null ? Map.of() : Map.copyOf(scopes);
    }

    public boolean appliesTo(Long productId, Long categoryId) {
        if (scopeType == PromotionScopeType.CART) {
            return true;
        }
        ScopeRefType refType = scopeType == PromotionScopeType.PRODUCT
                ? ScopeRefType.PRODUCT : ScopeRefType.CATEGORY;
        Long refId = refType == ScopeRefType.PRODUCT ? productId : categoryId;
        return refId != null && scopes.getOrDefault(refType, Set.of()).contains(refId);
    }
}
