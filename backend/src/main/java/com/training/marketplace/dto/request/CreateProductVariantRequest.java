package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Create a variant (SKU / mẫu mã) under a product")
public record CreateProductVariantRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 50)
        String sku,

        @NotBlank(message = "Variant name is required")
        @Size(max = 255)
        String variantName,

        @Size(max = 50) String color,
        @Size(max = 50) String size,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price must be non-negative")
        BigDecimal price,

        @Size(max = 500) String imageUrl,

        @Min(0) Integer minStock,
        @Min(0) Integer maxStock,
        @Min(0) Integer reorderPoint,
        @Min(0) Integer reorderQuantity
) {}
