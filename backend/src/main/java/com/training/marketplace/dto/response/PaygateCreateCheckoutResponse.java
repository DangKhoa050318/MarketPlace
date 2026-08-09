package com.training.marketplace.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaygateCreateCheckoutResponse(
        Integer code,
        Boolean success,
        String status,
        String message,
        PaygateCheckoutData data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PaygateCheckoutData(
            String token,
            String paymentUrl,
            String vietQrUrl,
            String paymentMethod,
            String qrCodePayload,
            String transferContent,
            String bankCode,
            String accountNumber,
            String accountName,
            String expiresAt
    ) {}

    public record BankAccountData(
            String bankName,
            String accountNumber,
            String accountHolder,
            BigDecimal amount
    ) {}
}
