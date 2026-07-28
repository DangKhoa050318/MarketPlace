package com.training.marketplace.dto.request;

import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Partial update of a coupon (ADMIN). {@code code} is the immutable business key and is absent.
 * Non-null fields are applied. If {@code scopeType} and/or {@code scopes} are provided, the scope
 * set is replaced. {@code usedCount} is never client-editable.
 */
@Schema(description = "Request payload for updating a coupon (code immutable)")
public record UpdatePromotionCodeRequest(
        DiscountType discountType,

        @DecimalMin(value = "0.01", message = "Discount value must be positive")
        BigDecimal discountValue,

        @DecimalMin(value = "0.01", message = "Max discount must be positive when set")
        BigDecimal maxDiscount,

        @DecimalMin(value = "0.00", message = "Minimum order amount cannot be negative")
        BigDecimal minOrderAmount,

        PromotionScopeType scopeType,

        @Valid
        List<ScopeRefDto> scopes,

        @Min(value = 1, message = "Usage limit must be at least 1")
        Integer usageLimit,

        @Min(value = 1, message = "Per-user limit must be at least 1")
        Integer perUserLimit,

        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        Boolean active
) {}
