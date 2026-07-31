package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Create a campaign (ADMIN). Starts in DRAFT; effective visibility is computed server-side from
 * status + [startsAt, endsAt]. {@code promotionCodeId} optionally surfaces a coupon (does not auto-apply).
 */
@Schema(description = "Request payload for creating a campaign")
public record CreateCampaignRequest(
        @NotBlank(message = "Campaign name is required")
        @Size(max = 150, message = "Campaign name must be at most 150 characters")
        String name,

        String description,

        LocalDateTime startsAt,
        LocalDateTime endsAt,

        @Schema(description = "Optional coupon id this campaign surfaces (not auto-applied)")
        Long promotionCodeId
) {}
