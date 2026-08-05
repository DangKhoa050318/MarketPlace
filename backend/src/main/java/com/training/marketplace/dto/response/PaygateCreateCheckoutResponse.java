package com.training.marketplace.dto.response;

import java.math.BigDecimal;

public record PaygateCreateCheckoutResponse(
        Integer code,
        Boolean success,
        String status,
        String message,
        PaygateCheckoutData data
) {
    public record PaygateCheckoutData(
            String token,
            String paymentUrl,
            String method,
            BankAccountData bankAccount,
            String transferContent,
            String qrPayload,
            String expiresAt
    ) {}

    public record BankAccountData(
            String bankName,
            String accountNumber,
            String accountHolder,
            BigDecimal amount
    ) {}
}
