package com.training.marketplace.dto.request;

import jakarta.validation.constraints.Size;

/** Partial update. {@code code} is the immutable business key and is therefore absent. */
public record UpdateCategoryRequest(
        @Size(min = 1, max = 100, message = "Category name must be between 1 and 100 characters")
        String name,

        @Size(min = 1, max = 100, message = "Category slug must be between 1 and 100 characters")
        String slug,

        Long parentId
) {}
