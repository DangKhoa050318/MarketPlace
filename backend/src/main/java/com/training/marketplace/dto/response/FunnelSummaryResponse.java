package com.training.marketplace.dto.response;

import java.time.Instant;
import java.util.List;

public record FunnelSummaryResponse(
        Instant from,
        Instant to,
        Long categoryId,
        Long productId,
        String campaign,
        String deviceType,
        List<FunnelStepResponse> steps
) {
}
