package com.training.marketplace.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Partial update for a variant. {@code sku} is the immutable business key. */
public record UpdateProductVariantRequest(
        @Size(max = 255) String variantName,
        @Size(max = 50) String color,
        @Size(max = 50) String size,

        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
        BigDecimal price,

        @Size(max = 500) String imageUrl,

        @Min(0) Integer minStock,
        @Min(0) Integer maxStock,
        @Min(0) Integer reorderPoint,
        @Min(0) Integer reorderQuantity,

        Boolean active
) {}
