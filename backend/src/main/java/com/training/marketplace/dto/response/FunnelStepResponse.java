package com.training.marketplace.dto.response;

public record FunnelStepResponse(
        String step,
        long count,
        double conversionRate,
        double dropOffRate
) {
}
