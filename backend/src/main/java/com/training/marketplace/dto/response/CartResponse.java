package com.training.marketplace.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartResponse(
        Long userId,
        List<CartItemResponse> items,
        BigDecimal totalAmount,
        BigDecimal shippingFee,
        Integer totalItems
) {}
