package com.training.marketplace.dto.response;

import com.training.marketplace.enums.ReturnRequestStatus;

import java.time.LocalDateTime;
import java.util.List;

public record ReturnRequestResponse(
        Long id,
        Long orderId,
        Long userId,
        Long orderItemId,
        Integer quantity,
        ReturnRequestStatus status,
        String reason,
        List<String> evidenceImageUrls,
        String adminNote,
        String qcNote,
        boolean refundWithoutReturn,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
