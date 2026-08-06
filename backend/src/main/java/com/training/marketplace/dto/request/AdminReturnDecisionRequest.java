package com.training.marketplace.dto.request;

public record AdminReturnDecisionRequest(
        boolean approved,
        boolean refundWithoutReturn,
        String note
) {
}
