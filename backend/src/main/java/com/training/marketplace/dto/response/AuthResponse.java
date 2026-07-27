package com.training.marketplace.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String username,
        String role
) {}
