package com.training.marketplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "One ranked and storefront-eligible recommendation")
public record RecommendationItemResponse(
        @Schema(description = "Zero-based position used by recommendation analytics", example = "0")
        int position,

        ProductResponse product,

        @Schema(description = "Strategy-specific ranking score", example = "0.85")
        double score,

        @Schema(description = "Machine-readable explanation of the ranking signals")
        String reason
) {
}
