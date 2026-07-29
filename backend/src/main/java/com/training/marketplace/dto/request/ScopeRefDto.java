package com.training.marketplace.dto.request;

import com.training.marketplace.enums.ScopeRefType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * One entry in a coupon's scope: a product (SPU) id or a category id. Used both when creating a
 * coupon (request) and when returning it ({@code PromotionCodeResponse}).
 */
@Schema(description = "A product or category reference inside a coupon scope")
public record ScopeRefDto(
        @Schema(description = "PRODUCT (products.id) or CATEGORY (categories.id)", example = "PRODUCT")
        @NotNull(message = "Scope refType is required")
        ScopeRefType refType,

        @Schema(description = "Target product/category id", example = "1")
        @NotNull(message = "Scope refId is required")
        Long refId
) {}
