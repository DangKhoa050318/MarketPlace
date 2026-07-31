package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Update a campaign's editable fields (ADMIN). Status transitions go through publish/archive endpoints. */
@Schema(description = "Request payload for updating a campaign")
public record UpdateCampaignRequest(
        @NotBlank(message = "Campaign name is required")
        @Size(max = 150, message = "Campaign name must be at most 150 characters")
        String name,

        String description,

        LocalDateTime startsAt,
        LocalDateTime endsAt,

        Long promotionCodeId
) {}
