package com.training.marketplace.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MovementItemResponse(
        Long id,
        Long variantId,
        String sku,
        String variantName,
        Integer quantity,
        BigDecimal unitCost,
        String batchNumber,
        LocalDate expiryDate,
        String notes
) {}
