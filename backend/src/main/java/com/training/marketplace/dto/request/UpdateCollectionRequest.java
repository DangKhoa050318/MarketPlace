package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Update a product collection's editable fields (ADMIN). Publish/unpublish go through separate endpoints. */
@Schema(description = "Request payload for updating a product collection")
public record UpdateCollectionRequest(
        @NotBlank(message = "Collection name is required")
        @Size(max = 150, message = "Collection name must be at most 150 characters")
        String name,

        @NotBlank(message = "Slug is required")
        @Size(max = 160, message = "Slug must be at most 160 characters")
        @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
                message = "Slug must be lowercase alphanumeric words separated by single hyphens")
        String slug,

        String description
) {}
