package com.training.marketplace.service;

import com.training.marketplace.dto.response.PaygateCreateCheckoutResponse;
import java.math.BigDecimal;

public interface PaygateClientService {
    PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description);

    PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description, String method);

    /** BNPL-specific: upfrontAmount + financeAmount must equal amount. */
    PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description,
                                                        String method, BigDecimal upfrontAmount, BigDecimal financeAmount);

    PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description,
                                                        String method, BigDecimal upfrontAmount, BigDecimal financeAmount,
                                                        Long merchantCustomerId, String customerName);
}
