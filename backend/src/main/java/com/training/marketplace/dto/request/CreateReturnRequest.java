package com.training.marketplace.dto.request;

import java.util.List;

public record CreateReturnRequest(
        Long orderItemId,
        Integer quantity,
        String reason,
        List<String> evidenceImageUrls
) {
    public CreateReturnRequest(String reason) {
        this(null, null, reason, null);
    }
}
