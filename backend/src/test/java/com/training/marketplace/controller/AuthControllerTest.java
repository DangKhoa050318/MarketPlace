package com.training.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.LoginRequest;
import com.training.marketplace.dto.request.RefreshTokenRequest;
import com.training.marketplace.dto.response.AuthResponse;
import com.training.marketplace.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.Cookie;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock private AuthService authService;

    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(
                authService,
                "marketplace_refresh",
                false,
                "Lax",
                "/api/v1/auth",
                Duration.ofDays(7));
    }

    @Test
    void loginWritesHttpOnlyCookieAndDoesNotSerializeRefreshToken() throws Exception {
        AuthResponse auth = new AuthResponse("access", "refresh-secret", "alice", "CUSTOMER");
        when(authService.login(argThat(request -> request.username().equals("alice"))))
                .thenReturn(auth);
        MockHttpServletResponse response = new MockHttpServletResponse();

        ApiResponse<AuthResponse> result = controller.login(
                new LoginRequest("alice", "secret"), response);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie)
                .contains("marketplace_refresh=refresh-secret")
                .contains("Path=/api/v1/auth")
                .contains("HttpOnly")
                .contains("SameSite=Lax");
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        assertThat(objectMapper.writeValueAsString(result))
                .contains("accessToken")
                .doesNotContain("refresh-secret")
                .doesNotContain("refreshToken");
    }

    @Test
    void refreshReadsCookieAndRotatesIt() {
        AuthResponse auth = new AuthResponse("new-access", "new-refresh", "alice", "CUSTOMER");
        when(authService.refreshToken(argThat(request -> request.refreshToken().equals("old-refresh"))))
                .thenReturn(auth);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("marketplace_refresh", "old-refresh"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.refresh(request, response);

        verify(authService).refreshToken(new RefreshTokenRequest("old-refresh"));
        assertThat(response.getHeader("Set-Cookie"))
                .contains("marketplace_refresh=new-refresh")
                .contains("HttpOnly");
    }

    @Test
    void logoutRevokesCookieTokenAndExpiresCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("marketplace_refresh", "refresh-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.logout(request, response);

        verify(authService).logout(new RefreshTokenRequest("refresh-token"));
        assertThat(response.getHeader("Set-Cookie"))
                .contains("marketplace_refresh=")
                .contains("Max-Age=0")
                .contains("HttpOnly");
    }
}
