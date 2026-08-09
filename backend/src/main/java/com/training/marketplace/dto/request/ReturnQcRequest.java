package com.training.marketplace.dto.request;

public record ReturnQcRequest(
        boolean passed,
        String note
) {
}
