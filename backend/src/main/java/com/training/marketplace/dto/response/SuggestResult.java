package com.training.marketplace.dto.response;

import java.math.BigDecimal;

public record SuggestResult(
    Long id,
    String name,
    String imageUrl,
    BigDecimal minPrice
) {}
