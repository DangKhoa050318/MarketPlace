package com.training.marketplace.service;

import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.enums.DiscountType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** REQ-STP-T-301: unit tests for discount math (percent/fixed, cap, rounding, non-negative). */
class DiscountCalculationServiceTest {

    private final DiscountCalculationService service = new DiscountCalculationService();

    private PromotionCode percent(BigDecimal value, BigDecimal maxDiscount) {
        return PromotionCode.builder()
                .code("P").discountType(DiscountType.PERCENT).discountValue(value).maxDiscount(maxDiscount)
                .build();
    }

    private PromotionCode fixed(BigDecimal value) {
        return PromotionCode.builder()
                .code("F").discountType(DiscountType.FIXED).discountValue(value)
                .build();
    }

    @Test
    @DisplayName("PERCENT without cap: 10% of 500 = 50.00")
    void percent_noCap() {
        BigDecimal d = service.calculate(percent(new BigDecimal("10"), null), new BigDecimal("500.00"));
        assertThat(d).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("PERCENT with cap: 50% of 500 = 250, capped to 100.00")
    void percent_withCap() {
        BigDecimal d = service.calculate(percent(new BigDecimal("50"), new BigDecimal("100")), new BigDecimal("500.00"));
        assertThat(d).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("FIXED: flat 30 off 500 = 30.00")
    void fixed_flat() {
        BigDecimal d = service.calculate(fixed(new BigDecimal("30")), new BigDecimal("500.00"));
        assertThat(d).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("FIXED larger than subtotal is clamped so total never goes negative")
    void fixed_clampedToSubtotal() {
        BigDecimal d = service.calculate(fixed(new BigDecimal("1000")), new BigDecimal("200.00"));
        assertThat(d).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("PERCENT rounds HALF_UP to scale 2: 10% of 99.99 = 9.999 -> 10.00")
    void percent_roundsHalfUp() {
        BigDecimal d = service.calculate(percent(new BigDecimal("10"), null), new BigDecimal("99.99"));
        assertThat(d).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Zero / non-positive eligible subtotal yields zero discount")
    void zeroSubtotal() {
        assertThat(service.calculate(fixed(new BigDecimal("50")), BigDecimal.ZERO)).isEqualByComparingTo("0.00");
        assertThat(service.calculate(percent(new BigDecimal("10"), null), null)).isEqualByComparingTo("0.00");
    }
}
