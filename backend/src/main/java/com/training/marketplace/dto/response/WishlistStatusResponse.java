package com.training.marketplace.dto.response;

public record WishlistStatusResponse(
        Long productId,
        boolean wishlisted
) {
}
