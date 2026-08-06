package com.training.marketplace.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Startup guard for PayGate webhook signature enforcement: production must never run with the
 * public webhook endpoint unauthenticated.
 */
class WebhookSignatureConfigGuardTest {

    private WebhookSignatureConfigGuard guard(String profile, boolean requireSignature) {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles(profile);
        return new WebhookSignatureConfigGuard(env, requireSignature);
    }

    @Test
    void prodProfileWithSignatureDisabled_failsFast() {
        WebhookSignatureConfigGuard guard = guard("prod", false);

        assertThatThrownBy(guard::verifyProductionEnforcesSignature)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("require-signature");
    }

    @Test
    void prodProfileWithSignatureEnabled_startsNormally() {
        WebhookSignatureConfigGuard guard = guard("prod", true);

        assertThatCode(guard::verifyProductionEnforcesSignature).doesNotThrowAnyException();
    }

    @Test
    void nonProdProfileWithSignatureDisabled_isAllowed() {
        WebhookSignatureConfigGuard guard = guard("dev", false);

        assertThatCode(guard::verifyProductionEnforcesSignature).doesNotThrowAnyException();
    }
}
