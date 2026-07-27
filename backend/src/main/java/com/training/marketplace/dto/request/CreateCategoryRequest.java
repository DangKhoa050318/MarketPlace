package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Request payload for creating a category")
public record CreateCategoryRequest(
        @Schema(description = "Category display name", example = "Electronics")
        @NotBlank(message = "Category name is required")
        @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
        String name,

        @Schema(description = "Unique internal code (immutable)", example = "ELEC")
        @NotBlank(message = "Category code is required")
        @Size(min = 1, max = 20, message = "Category code must be between 1 and 20 characters")
        String code,

        @Schema(description = "Unique category URL slug", example = "electronics")
        @NotBlank(message = "Category slug is required")
        @Size(min = 1, max = 100, message = "Category slug must be between 1 and 100 characters")
        String slug,

        @Schema(description = "Optional parent category id (hierarchy)", example = "1")
        Long parentId
) {}
