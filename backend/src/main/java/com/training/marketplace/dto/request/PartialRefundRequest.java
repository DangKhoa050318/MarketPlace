package com.training.marketplace.dto.request;

public record PartialRefundRequest(
        Long orderItemId,
        Integer quantity,
        String reason
) {
}
