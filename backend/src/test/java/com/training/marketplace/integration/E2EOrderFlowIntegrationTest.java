package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.dto.request.LoginRequest;
import com.training.marketplace.dto.request.RegisterRequest;
import com.training.marketplace.dto.response.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
        AuthResponse registerResp = postForAuthResponse("/api/v1/auth/register", registerReq);
        assertThat(registerResp.accessToken()).isNotNull();

        // Step 2: Login as customer
        LoginRequest loginReq = new LoginRequest("e2euser@example.com", "Password123!");
        AuthResponse loginResp = postForAuthResponse("/api/v1/auth/login", loginReq);
        String userToken = loginResp.accessToken();

        // Step 3: Login as Admin user
        LoginRequest adminLoginReq = new LoginRequest("admin@marketplace.com", "admin123");
        AuthResponse adminLoginResp = postForAuthResponse("/api/v1/auth/login", adminLoginReq);
        String adminToken = adminLoginResp.accessToken();

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
        assertThat(getOrdersResp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(getOrdersResp.getBody()).contains("\"success\":true");
    }
}
