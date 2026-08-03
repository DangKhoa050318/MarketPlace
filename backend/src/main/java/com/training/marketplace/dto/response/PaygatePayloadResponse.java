package com.training.marketplace.dto.response;

import java.math.BigDecimal;

public record PaygatePayloadResponse(
        Long orderId,
        Long customerId,
        String merchantId,
        BigDecimal totalAmount,
        BigDecimal upfrontAmount,
        BigDecimal financeAmount,
        String paymentChannel,
        String paymentUrl
) {}
