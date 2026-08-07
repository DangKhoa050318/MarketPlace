package com.training.marketplace.service;

import com.training.marketplace.dto.response.PaygateCreateCheckoutResponse;
import java.math.BigDecimal;

public interface PaygateClientService {
    PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description);

    PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description, String method);

    void refund(String transactionRef, Long orderId, BigDecimal amount, String idempotencyKey);
}
