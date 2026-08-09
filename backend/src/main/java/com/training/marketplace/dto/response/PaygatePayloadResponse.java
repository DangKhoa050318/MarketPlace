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
        String paymentUrl,
        String vietQrUrl,
        PaygateCreateCheckoutResponse.BankAccountData bankAccount,
        String transferContent,
        String qrPayload
) {
    public PaygatePayloadResponse(Long orderId, Long customerId, String merchantId, BigDecimal totalAmount, BigDecimal upfrontAmount, BigDecimal financeAmount, String paymentChannel, String paymentUrl) {
        this(orderId, customerId, merchantId, totalAmount, upfrontAmount, financeAmount, paymentChannel, paymentUrl, null, null, null, null);
    }
}
