package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.LoginRequest;
import com.training.marketplace.dto.request.RefreshTokenRequest;
import com.training.marketplace.dto.request.RegisterRequest;
import com.training.marketplace.dto.response.AuthResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.Role;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.security.JwtTokenProvider;
import com.training.marketplace.service.AuthService;
import com.training.marketplace.service.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenStore refreshTokenStore;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("User", "username", request.username());
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .role(Role.CUSTOMER)
                .active(true)
                .build();

        userRepository.save(user);

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
        storeRefreshToken(user.getUsername(), refreshToken);

        return new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getRole().name());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String identifier = authentication.getName();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new BadRequestException("User not found"));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUsername());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUsername());
        storeRefreshToken(user.getUsername(), refreshToken);

        return new AuthResponse(accessToken, refreshToken, user.getUsername(), user.getRole().name());
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isTokenValid(request.refreshToken(), "refresh")) {
            throw new BadRequestException("Invalid refresh token");
        }

        String username = jwtTokenProvider.extractUsername(request.refreshToken());
        String currentTokenId = jwtTokenProvider.extractTokenId(request.refreshToken());
        if (currentTokenId == null || currentTokenId.isBlank()) {
            throw new BadRequestException("Invalid refresh token");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String accessToken = jwtTokenProvider.generateAccessToken(username);
        String refreshToken = jwtTokenProvider.generateRefreshToken(username);
        String newTokenId = jwtTokenProvider.extractTokenId(refreshToken);
        if (!refreshTokenStore.rotate(
                username, currentTokenId, newTokenId, jwtTokenProvider.getRefreshTokenTtl())) {
            throw new BadRequestException("Refresh token has been revoked or already used");
        }

        return new AuthResponse(accessToken, refreshToken, username, user.getRole().name());
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        if (!jwtTokenProvider.isTokenValid(request.refreshToken(), "refresh")) {
            return;
        }
        String tokenId = jwtTokenProvider.extractTokenId(request.refreshToken());
        if (tokenId == null || tokenId.isBlank()) {
            return;
        }
        refreshTokenStore.revoke(
                jwtTokenProvider.extractUsername(request.refreshToken()),
                tokenId);
    }

    private void storeRefreshToken(String username, String refreshToken) {
        refreshTokenStore.save(
                username,
                jwtTokenProvider.extractTokenId(refreshToken),
                jwtTokenProvider.getRefreshTokenTtl());
    }
}
