package com.training.marketplace.dto.request;

import com.training.marketplace.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreateOrderRequest(
        @NotBlank(message = "Shipping address is required") String shippingAddress,
        String note,
        String couponCode,   // optional coupon applied at checkout (FEATURE-STP-02)
        PaymentMethod paymentMethod,
        @PositiveOrZero(message = "Upfront amount cannot be negative") BigDecimal upfrontAmount,
        @PositiveOrZero(message = "Finance amount cannot be negative") BigDecimal financeAmount,
        Integer bnplMonths
) {
    public CreateOrderRequest(String shippingAddress, String note, String couponCode) {
        this(shippingAddress, note, couponCode, PaymentMethod.COD, null, null, null);
    }
}
