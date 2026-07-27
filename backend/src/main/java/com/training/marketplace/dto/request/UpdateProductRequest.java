package com.training.marketplace.dto.request;

import jakarta.validation.constraints.Size;

/** Partial update for a product (SPU). {@code slug} is the immutable business key. */
public record UpdateProductRequest(
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        String description,

        Long categoryId,

        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        Boolean active
) {}
