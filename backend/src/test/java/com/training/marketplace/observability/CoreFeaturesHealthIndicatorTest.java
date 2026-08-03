package com.training.marketplace.observability;

import com.training.marketplace.config.CoreFeatureProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CoreFeaturesHealthIndicatorTest {

    @Test
    void health_exposesAllCoreFeatureFlagStates() {
        CoreFeatureProperties properties = new CoreFeatureProperties();
        properties.setPromotions(false);

        var health = new CoreFeaturesHealthIndicator(properties).health();

        assertThat(health.getStatus().getCode()).isEqualTo("UP");
        assertThat(health.getDetails())
                .containsEntry("REVIEW_QA", true)
                .containsEntry("PROMOTIONS", false)
                .containsEntry("RECOMMENDATIONS", true)
                .containsEntry("JOURNEY_ANALYTICS", true);
    }
}
