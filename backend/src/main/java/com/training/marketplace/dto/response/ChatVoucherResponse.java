package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChatVoucherResponse(
        Long campaignId,
        String campaignName,
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal maxDiscount,
        BigDecimal minOrderAmount,
        String scopeType,
        LocalDateTime expiresAt,
        String discountText
) {
}
