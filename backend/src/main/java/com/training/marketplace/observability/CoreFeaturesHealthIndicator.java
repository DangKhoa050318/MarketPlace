package com.training.marketplace.observability;

import com.training.marketplace.config.CoreFeature;
import com.training.marketplace.config.CoreFeatureProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("coreFeatures")
@RequiredArgsConstructor
public class CoreFeaturesHealthIndicator implements HealthIndicator {

    private final CoreFeatureProperties featureProperties;

    @Override
    public Health health() {
        Health.Builder health = Health.up();
        for (CoreFeature feature : CoreFeature.values()) {
            health.withDetail(feature.name(), featureProperties.enabled(feature));
        }
        return health.build();
    }
}
