package com.training.marketplace.entity;

import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A coupon / promotion code (FEATURE-STP-02). The {@code code} is the immutable business key,
 * stored normalized to upper-case. Concurrency on {@code usedCount} is guarded by a pessimistic
 * lock at order time (see {@code PromotionCodeRepository.findByCodeForUpdate}); {@code version} is
 * a defensive optimistic-lock column kept for consistency with the other inventory entities.
 * Foreign keys elsewhere reference this by plain {@code Long}. Scope rows live in
 * {@link PromotionCodeScope}; usage rows in {@link PromotionRedemption}.
 */
@Entity
@Table(name = "promotion_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionCode extends BaseEntity {

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 10)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    /** Cap for PERCENT discounts; null = no cap. Unused for FIXED. */
    @Column(name = "max_discount", precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 10)
    @Builder.Default
    private PromotionScopeType scopeType = PromotionScopeType.CART;

    /** Total redemptions allowed across all users; null = unlimited. */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    /** Redemptions allowed per user; null = unlimited. */
    @Column(name = "per_user_limit")
    private Integer perUserLimit;

    @Column(name = "used_count", nullable = false)
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
