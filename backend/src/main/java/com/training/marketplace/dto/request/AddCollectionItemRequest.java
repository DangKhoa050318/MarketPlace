package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Add one product to a collection (ADMIN). It is appended at the next display position. */
@Schema(description = "Request payload for adding a product to a collection")
public record AddCollectionItemRequest(
        @NotNull(message = "Product id is required")
        Long productId
) {}
