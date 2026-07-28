package com.training.marketplace.dto.response;

import java.time.LocalDateTime;

public record RecentlyViewedProductResponse(
        Long id,
        LocalDateTime viewedAt,
        ProductResponse product
) {
}
