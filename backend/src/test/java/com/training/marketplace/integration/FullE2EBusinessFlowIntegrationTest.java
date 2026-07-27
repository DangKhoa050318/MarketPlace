package com.training.marketplace.integration;

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

import static org.assertj.core.api.Assertions.assertThat;

class FullE2EBusinessFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Should execute complete E2E lifecycle: Register -> Cart -> Redis Eviction -> Admin Status Transition -> Access Control")
    void testCompleteE2EBusinessLifecycle() {
        // Step 1: Register customer user
        RegisterRequest registerReq = new RegisterRequest(
                "full_e2e_user",
                "fulle2e@example.com",
                "Password123!",
                "Full E2E User"
        );
        ResponseEntity<AuthResponse> registerResp = restTemplate.postForEntity(
                "/api/v1/auth/register",
                registerReq,
                AuthResponse.class
        );
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResp.getBody()).isNotNull();

        // Step 2: Login as Customer
        LoginRequest loginReq = new LoginRequest("fulle2e@example.com", "Password123!");
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginReq,
                AuthResponse.class
        );
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).isNotNull();
        String customerToken = loginResp.getBody().accessToken();

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

        // Step 5: Verify Admin Authorization & Access Control (Customer attempting admin endpoint receives 403)
        ResponseEntity<String> unauthorizedAdminResp = restTemplate.exchange(
                "/api/v1/admin/orders",
                HttpMethod.GET,
                customerVoidEntity,
                String.class
        );
        assertThat(unauthorizedAdminResp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        // Step 6: Login as Admin
        LoginRequest adminLoginReq = new LoginRequest("admin@orderflow.com", "admin123");
        ResponseEntity<AuthResponse> adminLoginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                adminLoginReq,
                AuthResponse.class
        );
        assertThat(adminLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminLoginResp.getBody()).isNotNull();
        String adminToken = adminLoginResp.getBody().accessToken();

        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        HttpEntity<Void> adminVoidEntity = new HttpEntity<>(adminHeaders);

        // Step 7: Admin fetches customer orders
        ResponseEntity<String> adminGetOrdersResp = restTemplate.exchange(
                "/api/v1/admin/orders?page=0&size=10",
                HttpMethod.GET,
                adminVoidEntity,
                String.class
        );
        assertThat(adminGetOrdersResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminGetOrdersResp.getBody()).contains("\"success\":true");

        // Step 8: Admin updates Order Status to CONFIRMED
        UpdateOrderStatusRequest updateStatusReq = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Order confirmed by admin");
        HttpEntity<UpdateOrderStatusRequest> updateStatusEntity = new HttpEntity<>(updateStatusReq, adminHeaders);

        ResponseEntity<String> updateStatusResp = restTemplate.exchange(
                "/api/v1/admin/orders/1/status",
                HttpMethod.PUT,
                updateStatusEntity,
                String.class
        );
        assertThat(updateStatusResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateStatusResp.getBody()).contains("\"status\":\"CONFIRMED\"");

        // Step 9: Edge Case - Invalid Status Transition (CONFIRMED -> PENDING) rejected with 400 Bad Request
        UpdateOrderStatusRequest invalidStatusReq = new UpdateOrderStatusRequest(OrderStatus.PENDING, "Reverting to pending");
        HttpEntity<UpdateOrderStatusRequest> invalidStatusEntity = new HttpEntity<>(invalidStatusReq, adminHeaders);

        ResponseEntity<String> invalidStatusResp = restTemplate.exchange(
                "/api/v1/admin/orders/1/status",
                HttpMethod.PUT,
                invalidStatusEntity,
                String.class
        );
        assertThat(invalidStatusResp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
