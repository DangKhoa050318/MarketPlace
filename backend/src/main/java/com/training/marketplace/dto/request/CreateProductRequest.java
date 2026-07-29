package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

@Schema(description = "Request payload for creating a product (SPU). Price/stock live on variants.")
public record CreateProductRequest(
        @Schema(description = "Unique product URL slug", example = "smartphone-pro-15")
        @NotBlank(message = "Product slug is required")
        @Size(max = 255, message = "Product slug must not exceed 255 characters")
        String slug,

        @Schema(description = "Product display name", example = "Smartphone Pro 15")
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        @Schema(description = "Detailed product description")
        String description,

        @Schema(description = "ID of the category this product belongs to", example = "1")
        @NotNull(message = "Category ID is required")
        Long categoryId,

        @Schema(description = "Product brand used for catalog grouping and recommendations")
        @Size(max = 120, message = "Brand must not exceed 120 characters")
        String brand,

        @Schema(description = "Searchable product-level attributes used for recommendations")
        @Size(max = 50, message = "A product cannot contain more than 50 attributes")
        Map<
                @NotBlank(message = "Attribute name cannot be blank")
                @Size(max = 80, message = "Attribute name must not exceed 80 characters")
                String,
                @NotBlank(message = "Attribute value cannot be blank")
                @Size(max = 255, message = "Attribute value must not exceed 255 characters")
                String> attributes,

        @Schema(description = "Unit of measure", example = "PCS")
        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        @Schema(description = "URL of the main product image")
        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl
) {
    public CreateProductRequest {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }

    public CreateProductRequest(
            String slug,
            String name,
            String description,
            Long categoryId,
            String unit,
            String imageUrl) {
        this(slug, name, description, categoryId, null, Map.of(), unit, imageUrl);
    }
}
