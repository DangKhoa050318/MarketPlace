package com.training.marketplace.dto.request;

import jakarta.validation.constraints.Positive;

public record ChatPageContext(
        @Positive Long productId,
        @Positive Long categoryId
) {
}
