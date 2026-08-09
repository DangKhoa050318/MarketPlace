package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.PaygateCreateCheckoutRequest;
import com.training.marketplace.dto.response.PaygateCreateCheckoutResponse;
import com.training.marketplace.exception.BadRequestException;
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

@Service
@Slf4j
public class PaygateClientServiceImpl implements PaygateClientService {

    private final RestTemplate restTemplate;
    private final CurrencyConversionService currencyConversionService;

    @Value("${marketplace.paygate.api-url:http://localhost:8081}")
    private String apiUrl;

    @Value("${marketplace.paygate.checkout-url:http://localhost:4201/checkout}")
    private String checkoutUrl;

    @Value("${marketplace.paygate.api-key:marketplace-api-key-123456}")
    private String apiKey;

    @Value("${marketplace.paygate.merchant-code:MARKETPLACE_MP}")
    private String merchantCode;

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
        return createCheckoutSession(orderId, amount, description, "WALLET", null, null);
    }

    @Override
    public PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description, String method) {
        return createCheckoutSession(orderId, amount, description, method, null, null);
    }

    @Override
    public PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description,
                                                               String method, BigDecimal upfrontAmount, BigDecimal financeAmount) {
        return createCheckoutSession(orderId, amount, description, method, upfrontAmount, financeAmount, null, null);
    }

    @Override
    public PaygateCreateCheckoutResponse createCheckoutSession(Long orderId, BigDecimal amount, String description,
                                                               String method, BigDecimal upfrontAmount, BigDecimal financeAmount,
                                                               Long merchantCustomerId, String customerName) {
        String fullEndpoint = apiUrl + "/api/v1/checkout/create";
        String orderIdStr = "ORD-" + orderId;
        BigDecimal vndAmount = currencyConversionService.convertUsdToVnd(amount);
        String paymentMethodStr = (method != null && !method.isBlank()) ? method : "WALLET";

        // Convert upfront/finance amounts to VND for BNPL sessions
        BigDecimal vndUpfront = upfrontAmount != null ? currencyConversionService.convertUsdToVnd(upfrontAmount) : null;
        BigDecimal vndFinance = financeAmount != null ? currencyConversionService.convertUsdToVnd(financeAmount) : null;

        String dynamicReturnUrl = returnUrl;
        String dynamicCancelUrl = cancelUrl != null && !cancelUrl.isBlank() ? cancelUrl : returnUrl + "?status=CANCELLED";

        PaygateCreateCheckoutRequest requestBody = new PaygateCreateCheckoutRequest(
                orderIdStr,
                vndAmount,
                description != null ? description : "Thanh toan don hang #" + orderId + " tren Marketplace",
                paymentMethodStr,
                vndUpfront,
                vndFinance,
                merchantCustomerId != null ? merchantCustomerId.toString() : null,
                customerName,
                dynamicReturnUrl,
                dynamicCancelUrl
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String jsonBody;
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            jsonBody = mapper.writeValueAsString(requestBody);
            String signature = com.training.marketplace.utils.HmacUtils.generateSignature(jsonBody, apiKey);
            headers.set("X-Merchant-Code", merchantCode);
            headers.set("X-Signature", signature);
        } catch (Exception e) {
            log.error("Failed to generate signature for request", e);
            throw new BadRequestException("Cannot create checkout session due to signature error");
        }

        HttpEntity<String> entity = new HttpEntity<>(jsonBody, headers);

        try {
            log.info("Sending checkout session creation request to PayGate: endpoint={}, orderId={}, method={}, upfront={}, finance={}",
                    fullEndpoint, orderIdStr, paymentMethodStr, vndUpfront, vndFinance);
            PaygateCreateCheckoutResponse response = restTemplate.postForObject(fullEndpoint, entity, PaygateCreateCheckoutResponse.class);

            boolean isSuccess = response != null && (
                    Boolean.TRUE.equals(response.success()) ||
                    "SUCCESS".equalsIgnoreCase(response.status()) ||
                    (response.code() != null && response.code() == 200)
            ) && response.data() != null;

            if (isSuccess) {
                log.info("Successfully created PayGate session: token={}, paymentUrl={}, method={}",
                        response.data().token(), response.data().paymentUrl(), response.data().method());
                return response;
            }
            log.warn("PayGate returned non-success response: {}", response);
        } catch (Exception e) {
            log.warn("Failed to reach PayGate service at {}. Error: {}", fullEndpoint, e.getMessage());
        }

        throw new BadRequestException("Cannot create PayGate checkout session. Please make sure PayGate backend is running on port 8081.");
    }
}
