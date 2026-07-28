package com.training.marketplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Body for the coupon preview endpoint. The backend reads the caller's cart itself. */
@Schema(description = "Request payload for previewing a coupon against the current cart")
public record ApplyCouponRequest(
        @Schema(example = "SUMMER10")
        @NotBlank(message = "Coupon code is required")
        String code
) {}
