package com.training.marketplace.dto.response;

import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.analytics.RecommendationStrategyType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Recommendation result and correlation context for analytics")
public record RecommendationResponse(
        @Schema(
                description = "Correlation ID to send with impression and click events",
                example = "93fc3727-47ae-4cbe-88ee-f9934753deca")
        UUID requestId,

        RecommendationPlacement placement,

        RecommendationStrategyType strategy,

        @Schema(description = "UTC time at which this result was generated")
        Instant generatedAt,

        List<RecommendationItemResponse> items
) {
    public RecommendationResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
