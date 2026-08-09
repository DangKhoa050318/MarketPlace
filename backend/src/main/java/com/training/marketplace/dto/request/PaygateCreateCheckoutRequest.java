package com.training.marketplace.dto.request;

import java.math.BigDecimal;

public record PaygateCreateCheckoutRequest(
        String orderId,
        BigDecimal amount,
        String description,
        String method,
        BigDecimal upfrontAmount,
        BigDecimal financeAmount,
        String merchantCustomerRef,
        String customerName,
        String returnUrl,
        String cancelUrl
) {
    /** Convenience constructor for non-BNPL methods (no split amounts). */
    public PaygateCreateCheckoutRequest(String orderId, BigDecimal amount,
                                        String description, String method,
                                        String returnUrl, String cancelUrl) {
        this(orderId, amount, description, method, null, null, null, null, returnUrl, cancelUrl);
    }
}
