package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.dto.request.LoginRequest;
import com.training.marketplace.dto.request.RegisterRequest;
import com.training.marketplace.dto.response.AuthResponse;
import com.training.marketplace.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class E2EOrderFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void testEndToEndOrderFlow() {
        // Step 1: Register customer user
        RegisterRequest registerReq = new RegisterRequest(
                "e2euser",
                "e2euser@example.com",
                "Password123!",
                "E2E Test User"
        );
        ResponseEntity<AuthResponse> registerResp = restTemplate.postForEntity(
                "/api/v1/auth/register",
                registerReq,
                AuthResponse.class
        );
        assertThat(registerResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResp.getBody()).isNotNull();
        assertThat(registerResp.getBody().accessToken()).isNotNull();

        // Step 2: Login as customer
        LoginRequest loginReq = new LoginRequest("e2euser@example.com", "Password123!");
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginReq,
                AuthResponse.class
        );
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResp.getBody()).isNotNull();
        String userToken = loginResp.getBody().accessToken();

        // Step 3: Login as Admin user
        LoginRequest adminLoginReq = new LoginRequest("admin@marketplace.com", "admin123");
        ResponseEntity<AuthResponse> adminLoginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                adminLoginReq,
                AuthResponse.class
        );
        assertThat(adminLoginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(adminLoginResp.getBody()).isNotNull();
        String adminToken = adminLoginResp.getBody().accessToken();

        // Step 4: Admin queries admin orders endpoint
        HttpHeaders adminHeaders = new HttpHeaders();
        adminHeaders.setBearerAuth(adminToken);
        HttpEntity<Void> adminEntity = new HttpEntity<>(adminHeaders);

        ResponseEntity<String> getOrdersResp = restTemplate.exchange(
                "/api/v1/admin/orders?page=0&size=10",
                HttpMethod.GET,
                adminEntity,
                String.class
        );
        assertThat(getOrdersResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getOrdersResp.getBody()).contains("\"success\":true");
    }
}
