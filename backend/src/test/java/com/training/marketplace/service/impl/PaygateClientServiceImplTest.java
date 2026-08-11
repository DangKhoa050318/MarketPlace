package com.training.marketplace.service.impl;

import com.training.marketplace.utils.HmacUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PaygateClientServiceImplTest {

    @Test
    void simulateBankTransferSignsTheExactJsonBodySentToPayGate() {
        PaygateClientServiceImpl service = new PaygateClientServiceImpl();
        String bankWebhookSecret = "test-bank-webhook-secret";
        ReflectionTestUtils.setField(service, "apiUrl", "http://paygate.test");
        ReflectionTestUtils.setField(service, "bankWebhookSecret", bankWebhookSecret);

        RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(service, "restTemplate");
        assertThat(restTemplate).isNotNull();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("http://paygate.test/api/v1/integration/bank-webhook"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(request -> {
                    MockClientHttpRequest mockRequest = (MockClientHttpRequest) request;
                    String rawBody = mockRequest.getBodyAsString(StandardCharsets.UTF_8);
                    assertThat(request.getHeaders().getFirst("X-Bank-Signature"))
                            .isEqualTo(HmacUtils.generateSignature(rawBody, bankWebhookSecret));
                    assertThat(rawBody).contains("\"transferContent\":\"PAYGATE ORD-34\"");
                })
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        service.simulateBankTransfer(34L, new BigDecimal("125000"));

        server.verify();
    }
}
