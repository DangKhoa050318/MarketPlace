package com.training.marketplace.messaging.event;

import com.training.marketplace.enums.MovementType;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record StockMovementCompletedEvent(
        String schemaVersion,
        String eventId,
        Long movementId,
        String movementReference,
        MovementType movementType,
        List<Long> variantIds,
        List<Long> warehouseIds,
        Instant occurredAt
) implements StockEvent {

    public static final String CURRENT_SCHEMA_VERSION = "1.0";

    public StockMovementCompletedEvent {
        schemaVersion = requireText(schemaVersion, "schemaVersion");
        eventId = requireText(eventId, "eventId");
        movementId = Objects.requireNonNull(movementId, "movementId is required");
        movementReference = requireText(movementReference, "movementReference");
        movementType = Objects.requireNonNull(movementType, "movementType is required");
        variantIds = immutableIds(variantIds, "variantIds");
        warehouseIds = immutableIds(warehouseIds, "warehouseIds");
        occurredAt = Objects.requireNonNull(occurredAt, "occurredAt is required");
    }

    public static StockMovementCompletedEvent create(
            Long movementId,
            String movementReference,
            MovementType movementType,
            Collection<Long> variantIds,
            Collection<Long> warehouseIds) {
        return new StockMovementCompletedEvent(
                CURRENT_SCHEMA_VERSION,
                UUID.randomUUID().toString(),
                movementId,
                movementReference,
                movementType,
                List.copyOf(variantIds),
                List.copyOf(warehouseIds),
                Instant.now());
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static List<Long> immutableIds(List<Long> ids, String field) {
        if (ids == null || ids.isEmpty() || ids.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(field + " must contain non-null values");
        }
        return List.copyOf(ids);
    }
}
