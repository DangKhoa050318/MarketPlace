package com.training.marketplace.dto.request;

import com.training.marketplace.enums.MerchandisingEventType;
import com.training.marketplace.enums.MerchandisingTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Record one impression/click (B-407). {@code eventId} is a client-generated UUID used for idempotent
 * dedup — replaying the same eventId is a no-op. The user is taken from the security context, not the body.
 */
@Schema(description = "Request payload for recording a merchandising impression/click")
public record RecordMerchandisingEventRequest(
        @NotNull(message = "eventId is required")
        UUID eventId,

        @NotNull(message = "eventType is required")
        MerchandisingEventType eventType,

        @NotNull(message = "targetType is required")
        MerchandisingTargetType targetType,

        @NotNull(message = "targetId is required")
        Long targetId,

        @Schema(description = "Optional anonymous session id for attribution")
        String sessionId
) {}
