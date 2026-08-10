package com.training.marketplace.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    @Test
    void validateJwtSecret_whenSecretMissing_failsFast() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "");

        assertThatThrownBy(provider::validateJwtSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET must be configured");
    }

    @Test
    void validateJwtSecret_whenSecretInvalid_failsFastWithActionableMessage() {
        JwtTokenProvider provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "jwtSecret", "not-a-valid-base64-hs512-key");

        assertThatThrownBy(provider::validateJwtSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 256 bits");
    }

    @Test
    void validateJwtSecret_whenSecretIs256Bits_acceptsIt() {
        JwtTokenProvider provider = new JwtTokenProvider();
        String validSecret = Base64.getEncoder().encodeToString(new byte[32]);
        ReflectionTestUtils.setField(provider, "jwtSecret", validSecret);

        assertThatCode(provider::validateJwtSecret).doesNotThrowAnyException();
    }
}
