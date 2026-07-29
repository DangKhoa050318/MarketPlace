package com.training.marketplace.dto.response;

import com.training.marketplace.dto.request.ScopeRefDto;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PromotionCodeResponse(
        Long id,
        String code,
        DiscountType discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscount,
        BigDecimal minOrderAmount,
        PromotionScopeType scopeType,
        List<ScopeRefDto> scopes,
        Integer usageLimit,
        Integer perUserLimit,
        Integer usedCount,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
