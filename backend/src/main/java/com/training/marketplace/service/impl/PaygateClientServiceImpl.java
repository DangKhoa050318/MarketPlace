package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.PaygateCreateCheckoutRequest;
import com.training.marketplace.dto.request.PaygateRefundRequest;
import com.training.marketplace.dto.response.PaygateCreateCheckoutResponse;
import com.training.marketplace.service.PaygateClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.training.marketplace.service.CurrencyConversionService;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Slf4j
public class PaygateClientServiceImpl implements PaygateClientService {

    private final RestTemplate restTemplate;
    private final CurrencyConversionService currencyConversionService;

    @Value("${marketplace.paygate.api-url:http://localhost:8081}")
    private String apiUrl;

    @Value("${marketplace.paygate.checkout-url:http://localhost:4201/checkout}")
    private String checkoutUrl;

    @Value("${marketplace.paygate.api-key:mock-merchant-api-key-123456}")
    private String apiKey;

    @Value("${marketplace.paygate.return-url:http://localhost:4200/orders/callback}")
    private String returnUrl;

    @Value("${marketplace.paygate.cancel-url:http://localhost:4200/orders/callback}")
    private String cancelUrl;

    public PaygateClientServiceImpl(CurrencyConversionService currencyConversionService) {
        this.restTemplate = new RestTemplate();
        this.currencyConversionService = currencyConversionService;
    }

    @Override
    public PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description) {
        return createCheckoutSession(orderId, amount, description, "WALLET");
    }

    @Override
    public PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description, String method) {
        String fullEndpoint = apiUrl + "/api/v1/checkout/create";
        String orderIdStr = "ORD-" + orderId;
        BigDecimal vndAmount = currencyConversionService.convertUsdToVnd(amount);
        String paymentMethodStr = (method != null && !method.isBlank()) ? method : "WALLET";

        String dynamicReturnUrl = returnUrl;
        String dynamicCancelUrl = cancelUrl != null && !cancelUrl.isBlank() ? cancelUrl : returnUrl + "?status=CANCELLED";

        PaygateCreateCheckoutRequest requestBody = new PaygateCreateCheckoutRequest(
                apiKey,
                orderIdStr,
                vndAmount,
                description != null ? description : "Thanh toan don hang #" + orderId + " tren Marketplace",
                paymentMethodStr,
                dynamicReturnUrl,
                dynamicCancelUrl
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<PaygateCreateCheckoutRequest> entity = new HttpEntity<>(requestBody, headers);

        try {
            log.info("Sending checkout session creation request to PayGate: endpoint={}, orderId={}, method={}", fullEndpoint, orderIdStr, paymentMethodStr);
            PaygateCreateCheckoutResponse response = restTemplate.postForObject(fullEndpoint, entity, PaygateCreateCheckoutResponse.class);

            boolean isSuccess = response != null && (
                    Boolean.TRUE.equals(response.success()) ||
                    "SUCCESS".equalsIgnoreCase(response.status()) ||
                    (response.code() != null && response.code() == 200)
            ) && response.data() != null;

            if (isSuccess) {
                log.info("Successfully created PayGate session: token={}, paymentUrl={}, method={}", response.data().token(), response.data().paymentUrl(), response.data().method());
                return response;
            }
            log.warn("PayGate returned non-success response: {}", response);
        } catch (Exception e) {
            log.warn("Failed to reach PayGate service at {}. Error: {}. Falling back to simulation mode.", fullEndpoint, e.getMessage());
        }

        // Fallback for offline local dev mode
        String mockToken = "CHK_MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String fallbackPaymentUrl = checkoutUrl + "?token=" + mockToken;

        PaygateCreateCheckoutResponse.BankAccountData mockBankAccount = null;
        String transferContent = null;
        String qrPayload = null;

        if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethodStr)) {
            mockBankAccount = new PaygateCreateCheckoutResponse.BankAccountData(
                    "MBBank - Ngân hàng TMCP Quân Đội",
                    "8888999988",
                    "PAYGATE GATEWAY SYSTEM",
                    vndAmount
            );
            transferContent = "PAYGATE MOCK_MERCHANT " + orderIdStr;
            qrPayload = "PAYGATE|MOCK_MERCHANT|" + orderIdStr + "|" + vndAmount;
        }

        return new PaygateCreateCheckoutResponse(
                200,
                true,
                "SUCCESS",
                "PayGate Session Created",
                new PaygateCreateCheckoutResponse.PaygateCheckoutData(
                        mockToken,
                        fallbackPaymentUrl,
                        paymentMethodStr,
                        mockBankAccount,
                        transferContent,
                        qrPayload,
                        null
                )
        );
    }

    @Override
    public void refund(String transactionRef, Long orderId, BigDecimal amount, String reason, String idempotencyKey) {
        if (transactionRef == null || transactionRef.isBlank()) {
            throw new IllegalArgumentException("PayGate transaction reference is required for refund");
        }
        if (orderId == null) {
            throw new IllegalArgumentException("Marketplace order ID is required for PayGate refund");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("PayGate merchant API key is not configured; cannot request PayGate refund");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency key is required for PayGate refund");
        }

        String endpoint = apiUrl + "/api/v1/refunds";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Merchant-Api-Key", apiKey);
        headers.set("Idempotency-Key", idempotencyKey);

        PaygateRefundRequest request = new PaygateRefundRequest(
                apiKey,
                transactionRef,
                "ORD-" + orderId,
                currencyConversionService.convertUsdToVnd(amount),
                reason
        );

        log.info("Requesting PayGate merchant refund for transactionRef={}, orderId={}, amount={}, idempotencyKey={}",
                transactionRef, request.orderId(), request.amount(), idempotencyKey);
        ResponseEntity<String> response = restTemplate.postForEntity(endpoint, new HttpEntity<>(request, headers), String.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("PayGate refund failed with status " + response.getStatusCode().value());
        }
    }
}
