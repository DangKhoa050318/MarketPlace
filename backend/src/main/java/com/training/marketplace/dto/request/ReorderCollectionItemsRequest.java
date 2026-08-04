package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Reorder a collection's items (ADMIN, B-406). {@code productIds} is the full, ordered list of the
 * collection's products; the backend rewrites every display_order in one transaction.
 */
@Schema(description = "Ordered product ids defining the new collection order")
public record ReorderCollectionItemsRequest(
        @NotEmpty(message = "Ordered product ids are required")
        List<Long> productIds
) {}
