package com.training.marketplace.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "marketplace.features")
public class CoreFeatureProperties {

    private boolean reviewQa = true;
    private boolean promotions = true;
    private boolean recommendations = true;
    private boolean journeyAnalytics = true;

    public boolean enabled(CoreFeature feature) {
        return switch (feature) {
            case REVIEW_QA -> reviewQa;
            case PROMOTIONS -> promotions;
            case RECOMMENDATIONS -> recommendations;
            case JOURNEY_ANALYTICS -> journeyAnalytics;
        };
    }
}
