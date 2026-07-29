package com.training.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Partial update for a product (SPU). {@code slug} is the immutable business key. */
public record UpdateProductRequest(
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        String description,

        Long categoryId,

        @Size(max = 120, message = "Brand must not exceed 120 characters")
        String brand,

        @Size(max = 50, message = "A product cannot contain more than 50 attributes")
        Map<
                @NotBlank(message = "Attribute name cannot be blank")
                @Size(max = 80, message = "Attribute name must not exceed 80 characters")
                String,
                @NotBlank(message = "Attribute value cannot be blank")
                @Size(max = 255, message = "Attribute value must not exceed 255 characters")
                String> attributes,

        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl,

        Boolean active
) {
    public UpdateProductRequest {
        attributes = attributes == null ? null : Map.copyOf(attributes);
    }

    public UpdateProductRequest(
            String name,
            String description,
            Long categoryId,
            String unit,
            String imageUrl,
            Boolean active) {
        this(name, description, categoryId, null, null, unit, imageUrl, active);
    }
}
