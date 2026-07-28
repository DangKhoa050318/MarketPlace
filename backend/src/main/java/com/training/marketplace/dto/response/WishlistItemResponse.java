package com.training.marketplace.dto.response;

import java.time.LocalDateTime;

public record WishlistItemResponse(
        Long id,
        Long userId,
        LocalDateTime addedAt,
        ProductResponse product
) {
}
