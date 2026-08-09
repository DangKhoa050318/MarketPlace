package com.training.marketplace.dto.response;

import java.math.BigDecimal;

public record ChatProductCardResponse(
        Long productId,
        String slug,
        String name,
        String imageUrl,
        Long categoryId,
        String categoryName,
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        long availableStock,
        String reason,
        ChatVoucherResponse voucher
) {
}
