package com.training.marketplace.service;

import com.training.marketplace.dto.request.LoginRequest;
import com.training.marketplace.dto.request.RefreshTokenRequest;
import com.training.marketplace.dto.request.RegisterRequest;
import com.training.marketplace.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);
}
