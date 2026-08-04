package com.training.marketplace.dto.response;

import com.training.marketplace.enums.DeliveryEventType;
import com.training.marketplace.enums.DeliveryStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record DeliveryEventResponse(
        Long id,
        UUID requestId,
        DeliveryEventType eventType,
        DeliveryStatus status,
        String note,
        LocalDateTime occurredAt
) {
}
