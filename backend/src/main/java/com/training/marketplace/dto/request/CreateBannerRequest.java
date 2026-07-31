package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** Create a merchandising banner (ADMIN). Effective visibility is computed server-side from status + schedule. */
@Schema(description = "Request payload for creating a merchandising banner")
public record CreateBannerRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @NotBlank(message = "Desktop image URL is required")
        @Size(max = 500, message = "Desktop image URL must be at most 500 characters")
        String imageUrlDesktop,

        @Size(max = 500, message = "Mobile image URL must be at most 500 characters")
        String imageUrlMobile,

        @NotBlank(message = "Alt text is required")
        @Size(max = 255, message = "Alt text must be at most 255 characters")
        String altText,

        @Size(max = 500, message = "Target URL must be at most 500 characters")
        String targetUrl,

        @Schema(description = "Slot key, e.g. HOME_HERO")
        @NotBlank(message = "Position is required")
        @Size(max = 40, message = "Position must be at most 40 characters")
        String position,

        Integer displayOrder,

        LocalDateTime startsAt,
        LocalDateTime endsAt
) {}
