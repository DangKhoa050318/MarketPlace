package com.training.marketplace.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.dto.request.AddToCartRequest;
import com.training.marketplace.dto.request.LoginRequest;
import com.training.marketplace.dto.request.RegisterRequest;
import com.training.marketplace.dto.request.UpdateOrderStatusRequest;
import com.training.marketplace.dto.response.AuthResponse;
import com.training.marketplace.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FullE2EBusinessFlowIntegrationTest extends BaseIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Should execute complete E2E lifecycle: Register -> Cart -> Redis Eviction -> Admin Status Transition -> Access Control")
    void testCompleteE2EBusinessLifecycle() throws Exception {
        // Step 1: Register customer user
        RegisterRequest registerReq = new RegisterRequest(
                "full_e2e_user",
                "fulle2e@example.com",
                "Password123!",
                "Full E2E User"
        );
        AuthResponse registerResp = postForAuthResponse("/api/v1/auth/register", registerReq);
        assertThat(registerResp.accessToken()).isNotNull();

        // Step 2: Login as Customer
        LoginRequest loginReq = new LoginRequest("fulle2e@example.com", "Password123!");
        AuthResponse loginResp = postForAuthResponse("/api/v1/auth/login", loginReq);
        String customerToken = loginResp.accessToken();

        HttpHeaders customerHeaders = new HttpHeaders();
        customerHeaders.setBearerAuth(customerToken);

        // Step 3: Add product to Redis Cart
        AddToCartRequest addToCartReq = new AddToCartRequest(1L, 2);
        HttpEntity<AddToCartRequest> cartEntity = new HttpEntity<>(addToCartReq, customerHeaders);

        ResponseEntity<String> addCartResp = restTemplate.exchange(
                "/api/v1/cart/items",
                HttpMethod.POST,
                cartEntity,
                String.class
        );
        assertThat(addCartResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(addCartResp.getBody()).contains("\"success\":true");

        // Step 4: Verify Redis Cart Items
        HttpEntity<Void> customerVoidEntity = new HttpEntity<>(customerHeaders);
        ResponseEntity<String> getCartResp = restTemplate.exchange(
                "/api/v1/cart",
                HttpMethod.GET,
                customerVoidEntity,
                String.class
        );
        assertThat(getCartResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getCartResp.getBody()).contains("\"items\":[");

        // Step 5: Checkout the cart and keep the actual order id for subsequent transitions.
        HttpEntity<Map<String, Object>> createOrderEntity = new HttpEntity<>(
                Map.of(
                        "shippingAddress", "123 Full E2E Street",
                        "paymentMethod", "BANK_TRANSFER"),
                customerHeaders);
        ResponseEntity<String> createOrderResp = restTemplate.exchange(
                "/api/v1/orders",
                HttpMethod.POST,
                createOrderEntity,
                String.class
        );
        assertThat(createOrderResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode createdOrder = objectMapper.readTree(createOrderResp.getBody()).path("data");
        long orderId = createdOrder.path("id").asLong();
        assertThat(orderId).isPositive();

        // Step 6: Verify Admin Authorization & Access Control (Customer attempting admin endpoint receives 403)
        ResponseEntity<String> unauthorizedAdminResp = restTemplate.exchange(
                "/api/v1/admin/orders",
                HttpMethod.GET,
                customerVoidEntity,
                String.class
        );
        assertThat(unauthorizedAdminResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Step 7: Login as Admin
        LoginRequest adminLoginReq = new LoginRequest("admin@marketplace.com", "admin123");
        AuthResponse adminLoginResp = postForAuthResponse("/api/v1/auth/login", adminLoginReq);
        String adminToken = adminLoginResp.accessToken();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        HttpEntity<Void> adminVoidEntity = new HttpEntity<>(adminHeaders);

        // Step 8: Admin fetches customer orders
        ResponseEntity<String> adminGetOrdersResp = restTemplate.exchange(
                "/api/v1/admin/orders?page=0&size=10",
                HttpMethod.GET,
                adminVoidEntity,
                String.class
        );
        assertThat(adminGetOrdersResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminGetOrdersResp.getBody()).contains("\"success\":true");

        // Step 9: Admin updates Order Status to CONFIRMED
        UpdateOrderStatusRequest updateStatusReq = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Order confirmed by admin");
        HttpEntity<UpdateOrderStatusRequest> updateStatusEntity = new HttpEntity<>(updateStatusReq, adminHeaders);

        ResponseEntity<String> updateStatusResp = restTemplate.exchange(
                "/api/v1/admin/orders/" + orderId + "/status",
                HttpMethod.PUT,
                updateStatusEntity,
                String.class
        );
        assertThat(updateStatusResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateStatusResp.getBody()).contains("\"status\":\"CONFIRMED\"");

        // Step 10: Edge Case - Invalid Status Transition (CONFIRMED -> PENDING) rejected with 400 Bad Request
        UpdateOrderStatusRequest invalidStatusReq = new UpdateOrderStatusRequest(OrderStatus.PENDING, "Reverting to pending");
        HttpEntity<UpdateOrderStatusRequest> invalidStatusEntity = new HttpEntity<>(invalidStatusReq, adminHeaders);

        ResponseEntity<String> invalidStatusResp = restTemplate.exchange(
                "/api/v1/admin/orders/" + orderId + "/status",
                HttpMethod.PUT,
                invalidStatusEntity,
                String.class
        );
        assertThat(invalidStatusResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
