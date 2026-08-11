package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.LoginRequest;
import com.training.marketplace.dto.request.RefreshTokenRequest;
import com.training.marketplace.dto.request.RegisterRequest;
import com.training.marketplace.dto.response.AuthResponse;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Register, login, and refresh tokens")
public class AuthController {

    private final AuthService authService;
    private final String refreshCookieName;
    private final boolean refreshCookieSecure;
    private final String refreshCookieSameSite;
    private final String refreshCookiePath;
    private final Duration refreshCookieMaxAge;

    public AuthController(
            AuthService authService,
            @Value("${auth.refresh-cookie.name:marketplace_refresh}") String refreshCookieName,
            @Value("${auth.refresh-cookie.secure:false}") boolean refreshCookieSecure,
            @Value("${auth.refresh-cookie.same-site:Lax}") String refreshCookieSameSite,
            @Value("${auth.refresh-cookie.path:/api/v1/auth}") String refreshCookiePath,
            @Value("${auth.refresh-cookie.max-age:7d}") Duration refreshCookieMaxAge) {
        this.authService = authService;
        this.refreshCookieName = refreshCookieName;
        this.refreshCookieSecure = refreshCookieSecure;
        this.refreshCookieSameSite = refreshCookieSameSite;
        this.refreshCookiePath = refreshCookiePath;
        this.refreshCookieMaxAge = refreshCookieMaxAge;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletResponse response) {
        AuthResponse auth = authService.register(request);
        writeRefreshCookie(response, auth.refreshToken(), refreshCookieMaxAge);
        return ApiResponse.success("Registration successful", auth);
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        writeRefreshCookie(response, auth.refreshToken(), refreshCookieMaxAge);
        return ApiResponse.success("Login successful", auth);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh access token using the HttpOnly refresh cookie")
    public ApiResponse<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = readRefreshToken(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BadRequestException("Refresh token cookie is required");
        }
        AuthResponse auth = authService.refreshToken(new RefreshTokenRequest(refreshToken));
        writeRefreshCookie(response, auth.refreshToken(), refreshCookieMaxAge);
        return ApiResponse.success("Token refreshed", auth);
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke and clear the HttpOnly refresh cookie")
    public ApiResponse<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refreshToken = readRefreshToken(request);
        if (refreshToken != null && !refreshToken.isBlank()) {
            authService.logout(new RefreshTokenRequest(refreshToken));
        }
        writeRefreshCookie(response, "", Duration.ZERO);
        return ApiResponse.success("Logout successful", null);
    }

    private String readRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (refreshCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeRefreshCookie(
            HttpServletResponse response,
            String value,
            Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(refreshCookieName, value)
                .httpOnly(true)
                .secure(refreshCookieSecure)
                .sameSite(refreshCookieSameSite)
                .path(refreshCookiePath)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
