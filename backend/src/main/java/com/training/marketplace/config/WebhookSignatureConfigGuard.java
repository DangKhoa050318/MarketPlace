package com.training.marketplace.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

/**
 * Fail-fast startup guard for the PayGate webhook authentication.
 *
 * <p>The webhook endpoint ({@code /api/v1/payments/paygate-webhook}) is public ({@code permitAll}),
 * so the shared-secret {@code X-Paygate-Signature} check is its <em>only</em> authentication.
 * Turning it off ({@code marketplace.paygate.webhook.require-signature=false}) is a dev-only escape
 * hatch for PayGate builds that don't yet echo the secret — but if it slipped into production, an
 * unauthenticated caller could POST forged {@code CANCELLED}/{@code SUCCESS} webhooks and corrupt
 * stock (phantom stock).
 *
 * <p>This guard refuses to start the application when the {@code prod} profile is active and
 * signature enforcement is disabled, converting a silent production misconfiguration into a loud
 * boot-time failure.
 */
@Component
@Slf4j
public class WebhookSignatureConfigGuard {

    private final Environment environment;
    private final boolean requireSignature;

    public WebhookSignatureConfigGuard(
            Environment environment,
            @Value("${marketplace.paygate.webhook.require-signature:true}") boolean requireSignature) {
        this.environment = environment;
        this.requireSignature = requireSignature;
    }

    @PostConstruct
    void verifyProductionEnforcesSignature() {
        boolean prod = environment.acceptsProfiles(Profiles.of("prod"));
        if (prod && !requireSignature) {
            throw new IllegalStateException(
                    "Refusing to start: marketplace.paygate.webhook.require-signature must be true under "
                    + "the 'prod' profile. The PayGate webhook endpoint is public and this signature is its "
                    + "only authentication — disabling it lets forged webhooks corrupt inventory (phantom "
                    + "stock). Unset PAYGATE_WEBHOOK_REQUIRE_SIGNATURE (it defaults to true) or set it to true.");
        }
        if (!requireSignature) {
            log.warn("PayGate webhook signature enforcement is DISABLED. Acceptable only for local/dev where "
                    + "the PayGate build does not send the X-Paygate-Signature header; never enable in production.");
        }
    }
}
