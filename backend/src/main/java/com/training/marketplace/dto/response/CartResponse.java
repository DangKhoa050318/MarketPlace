package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long userId,
        List<CartItemResponse> items,
        BigDecimal totalAmount,
        Integer totalItems
) {}
