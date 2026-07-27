package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

        @Schema(description = "Unit of measure", example = "PCS")
        @Size(max = 20, message = "Unit must not exceed 20 characters")
        String unit,

        @Schema(description = "URL of the main product image")
        @Size(max = 500, message = "Image URL must not exceed 500 characters")
        String imageUrl
) {}
