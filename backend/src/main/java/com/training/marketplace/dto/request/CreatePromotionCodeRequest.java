package com.training.marketplace.dto.request;

import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Create a coupon (ADMIN). {@code code} is normalized to upper-case and is immutable afterwards.
 * When {@code scopeType} is PRODUCT/CATEGORY, {@code scopes} must be non-empty. Cross-field rules
 * (percent 1..100, scope requires refs) are enforced in the service.
 */
@Schema(description = "Request payload for creating a coupon")
public record CreatePromotionCodeRequest(
        @Schema(example = "SUMMER10")
        @NotBlank(message = "Coupon code is required")
        @Size(min = 1, max = 30, message = "Coupon code must be between 1 and 30 characters")
        String code,

        @NotNull(message = "Discount type is required")
        DiscountType discountType,

        @Schema(description = "Percent (1..100) when PERCENT, or money amount when FIXED", example = "10")
        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be positive")
        BigDecimal discountValue,

        @Schema(description = "Cap for PERCENT discounts; null = no cap")
        @DecimalMin(value = "0.01", message = "Max discount must be positive when set")
        BigDecimal maxDiscount,

        @Schema(description = "Minimum whole-cart subtotal to qualify; default 0")
        @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
        BigDecimal minOrderAmount,

        @Schema(description = "CART (default), PRODUCT or CATEGORY")
        PromotionScopeType scopeType,

        @Schema(description = "Required (non-empty) when scopeType is PRODUCT or CATEGORY")
        @Valid
        List<ScopeRefDto> scopes,

        @Schema(description = "Total redemptions across all users; null = unlimited")
        @Min(value = 1, message = "Usage limit must be at least 1")
        Integer usageLimit,

        @Schema(description = "Redemptions per user; null = unlimited")
        @Min(value = 1, message = "Per-user limit must be at least 1")
        Integer perUserLimit,

        LocalDateTime startsAt,
        LocalDateTime expiresAt,

        @Schema(description = "Defaults to true")
        Boolean active
) {}
