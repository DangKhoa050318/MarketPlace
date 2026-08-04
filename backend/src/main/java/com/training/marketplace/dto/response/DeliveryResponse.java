package com.training.marketplace.dto.response;

import com.training.marketplace.enums.DeliveryStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DeliveryResponse(
        Long id,
        Long orderId,
        Long version,
        String carrier,
        String trackingCode,
        DeliveryStatus status,
        LocalDate estimatedDelivery,
        LocalDateTime deliveredAt,
        List<DeliveryEventResponse> events,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
