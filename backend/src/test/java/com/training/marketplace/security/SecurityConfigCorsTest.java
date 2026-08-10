package com.training.marketplace.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SecurityConfigCorsTest {

    @Test
    void corsConfiguration_allowsOnlyConfiguredOriginsWithCredentials() {
        SecurityConfig securityConfig = new SecurityConfig(null, null);
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins",
                new String[]{"http://localhost:4200", "http://localhost:4201"});

        CorsConfiguration configuration = securityConfig.corsConfigurationSource()
                .getCorsConfiguration(preflightFrom("http://localhost:4200"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.checkOrigin("http://localhost:4200")).isEqualTo("http://localhost:4200");
        assertThat(configuration.checkOrigin("http://evil.test")).isNull();
        assertThat(configuration.getAllowedOriginPatterns()).isNull();
        assertThat(configuration.getAllowedOrigins()).doesNotContain("*");
    }

    @Test
    void corsConfiguration_rejectsWildcardOriginsWhenCredentialsAreAllowed() {
        SecurityConfig securityConfig = new SecurityConfig(null, null);
        ReflectionTestUtils.setField(securityConfig, "allowedOrigins", new String[]{"*"});

        assertThatThrownBy(securityConfig::corsConfigurationSource)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not contain '*'");
    }

    @Test
    @DisplayName("JWT and rate-limit filters are only registered inside the Spring Security chain")
    void servletFilterAutoRegistration_isDisabledForSecurityChainFilters() {
        SecurityConfig securityConfig = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(RateLimitingFilter.class));

        assertThat(securityConfig.jwtAuthenticationFilterRegistration().isEnabled()).isFalse();
        assertThat(securityConfig.rateLimitingFilterRegistration().isEnabled()).isFalse();
    }

    private HttpServletRequest preflightFrom(String origin) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/products");
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", "GET");
        return request;
    }
}
