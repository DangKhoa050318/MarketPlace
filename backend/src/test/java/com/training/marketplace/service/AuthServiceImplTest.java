package com.training.marketplace.service;

import com.training.marketplace.dto.request.RefreshTokenRequest;
import com.training.marketplace.dto.response.AuthResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.Role;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.security.JwtTokenProvider;
import com.training.marketplace.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenStore refreshTokenStore;

    @InjectMocks private AuthServiceImpl authService;

    @Test
    void refreshToken_rotatesServerSideTokenAndReturnsNewPair() {
        String oldToken = "old-refresh-token";
        String newToken = "new-refresh-token";
        Duration ttl = Duration.ofDays(7);
        User user = User.builder().username("alice").role(Role.CUSTOMER).active(true).build();

        when(jwtTokenProvider.isTokenValid(oldToken, "refresh")).thenReturn(true);
        when(jwtTokenProvider.extractUsername(oldToken)).thenReturn("alice");
        when(jwtTokenProvider.extractTokenId(oldToken)).thenReturn("old-jti");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken("alice")).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken("alice")).thenReturn(newToken);
        when(jwtTokenProvider.extractTokenId(newToken)).thenReturn("new-jti");
        when(jwtTokenProvider.getRefreshTokenTtl()).thenReturn(ttl);
        when(refreshTokenStore.rotate("alice", "old-jti", "new-jti", ttl)).thenReturn(true);

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest(oldToken));

        assertThat(response.accessToken()).isEqualTo("new-access-token");
        assertThat(response.refreshToken()).isEqualTo(newToken);
        verify(refreshTokenStore).rotate("alice", "old-jti", "new-jti", ttl);
    }

    @Test
    void refreshToken_reusedOrRevokedTokenIsRejected() {
        String oldToken = "old-refresh-token";
        String newToken = "new-refresh-token";
        Duration ttl = Duration.ofDays(7);
        User user = User.builder().username("alice").role(Role.CUSTOMER).active(true).build();

        when(jwtTokenProvider.isTokenValid(oldToken, "refresh")).thenReturn(true);
        when(jwtTokenProvider.extractUsername(oldToken)).thenReturn("alice");
        when(jwtTokenProvider.extractTokenId(oldToken)).thenReturn("old-jti");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken("alice")).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken("alice")).thenReturn(newToken);
        when(jwtTokenProvider.extractTokenId(newToken)).thenReturn("new-jti");
        when(jwtTokenProvider.getRefreshTokenTtl()).thenReturn(ttl);
        when(refreshTokenStore.rotate("alice", "old-jti", "new-jti", ttl)).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(oldToken)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("revoked or already used");
    }

    @Test
    void refreshToken_legacyTokenWithoutJtiIsRejected() {
        String token = "legacy-refresh-token";
        when(jwtTokenProvider.isTokenValid(token, "refresh")).thenReturn(true);
        when(jwtTokenProvider.extractUsername(token)).thenReturn("alice");
        when(jwtTokenProvider.extractTokenId(token)).thenReturn(null);

        assertThatThrownBy(() -> authService.refreshToken(new RefreshTokenRequest(token)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void logout_validRefreshTokenRevokesMatchingServerSideToken() {
        String token = "refresh-token";
        when(jwtTokenProvider.isTokenValid(token, "refresh")).thenReturn(true);
        when(jwtTokenProvider.extractUsername(token)).thenReturn("alice");
        when(jwtTokenProvider.extractTokenId(token)).thenReturn("jti");

        authService.logout(new RefreshTokenRequest(token));

        verify(refreshTokenStore).revoke("alice", "jti");
    }

    @Test
    void logout_invalidTokenIsIdempotent() {
        String token = "invalid";
        when(jwtTokenProvider.isTokenValid(token, "refresh")).thenReturn(false);

        authService.logout(new RefreshTokenRequest(token));

        verify(refreshTokenStore, never()).revoke("alice", "jti");
    }
}
