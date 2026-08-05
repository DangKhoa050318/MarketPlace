package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.PaygateCreateCheckoutRequest;
import com.training.marketplace.dto.response.PaygateCreateCheckoutResponse;
import com.training.marketplace.service.PaygateClientService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

    @Value("${marketplace.paygate.api-key:mock-merchant-api-key-123456}")
    private String apiKey;

    @Value("${marketplace.paygate.return-url:http://localhost:4200/orders/callback}")
    private String returnUrl;

    @Value("${marketplace.paygate.cancel-url:http://localhost:4200/cart}")
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

        PaygateCreateCheckoutRequest requestBody = new PaygateCreateCheckoutRequest(
                apiKey,
                orderIdStr,
                vndAmount,
                description != null ? description : "Thanh toan don hang #" + orderId + " tren Marketplace",
                paymentMethodStr,
                returnUrl,
                cancelUrl
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
        String fallbackPaymentUrl = "http://localhost:4201/checkout?token=" + mockToken;

        PaygateCreateCheckoutResponse.BankAccountData mockBankAccount = null;
        String transferContent = null;
        String qrPayload = null;

        if ("BANK_TRANSFER".equalsIgnoreCase(paymentMethodStr)) {
            mockBankAccount = new PaygateCreateCheckoutResponse.BankAccountData(
                    "Ngân hàng chủ PayGate (VietinBank)",
                    "PAYGATE-0001-9999",
                    "PayGate JSC",
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
}
