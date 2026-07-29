package com.training.marketplace.config;

import com.training.marketplace.enums.OrderStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationPropertiesTest {

    private static Validator validator;
    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withUserConfiguration(RecommendationConfig.class);

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void defaults_areValidAndUseThirtyDayLookback() {
        RecommendationProperties properties = new RecommendationProperties();

        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.getBestSellerLookback()).isEqualTo(Duration.ofDays(30));
        assertThat(properties.getValidOrderStatuses())
                .containsExactlyInAnyOrder(
                        OrderStatus.CONFIRMED,
                        OrderStatus.PROCESSING,
                        OrderStatus.SHIPPED,
                        OrderStatus.DELIVERED);
        assertThat(properties.getCoViewedLookback()).isEqualTo(Duration.ofDays(90));
        assertThat(properties.getMinCoViewedOccurrences()).isEqualTo(2);
        assertThat(properties.getCoPurchasedLookback()).isEqualTo(Duration.ofDays(90));
        assertThat(properties.getMinCoPurchasedOccurrences()).isEqualTo(2);
    }

    @Test
    void lookbackShorterThanOneDay_isRejected() {
        RecommendationProperties properties = new RecommendationProperties();
        properties.setBestSellerLookback(Duration.ofHours(12));

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getMessage()
                        .contains("at least one day"));
    }

    @Test
    void pendingAndCancelledStatuses_areRejected() {
        RecommendationProperties properties = new RecommendationProperties();
        properties.setValidOrderStatuses(
                EnumSet.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED));

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getMessage()
                        .contains("PENDING or CANCELLED"));
    }

    @Test
    void springBinding_supportsCustomDurationAndStatuses() {
        contextRunner
                .withPropertyValues(
                        "marketplace.recommendation.best-seller-lookback=14d",
                        "marketplace.recommendation.valid-order-statuses=SHIPPED,DELIVERED",
                        "marketplace.recommendation.co-viewed-lookback=21d",
                        "marketplace.recommendation.min-co-viewed-occurrences=3",
                        "marketplace.recommendation.co-purchased-lookback=120d",
                        "marketplace.recommendation.min-co-purchased-occurrences=4")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    RecommendationProperties properties =
                            context.getBean(RecommendationProperties.class);
                    assertThat(properties.getBestSellerLookback()).isEqualTo(Duration.ofDays(14));
                    assertThat(properties.getValidOrderStatuses())
                            .containsExactlyInAnyOrder(OrderStatus.SHIPPED, OrderStatus.DELIVERED);
                    assertThat(properties.getCoViewedLookback()).isEqualTo(Duration.ofDays(21));
                    assertThat(properties.getMinCoViewedOccurrences()).isEqualTo(3);
                    assertThat(properties.getCoPurchasedLookback()).isEqualTo(Duration.ofDays(120));
                    assertThat(properties.getMinCoPurchasedOccurrences()).isEqualTo(4);
                });
    }

    @Test
    void springBinding_rejectsInvalidStatusConfigurationAtStartup() {
        contextRunner
                .withPropertyValues(
                        "marketplace.recommendation.valid-order-statuses=DELIVERED,CANCELLED")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void coOccurrenceThresholdsBelowTwo_areRejected() {
        RecommendationProperties properties = new RecommendationProperties();
        properties.setMinCoViewedOccurrences(1);
        properties.setMinCoPurchasedOccurrences(1);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("minCoViewedOccurrences", "minCoPurchasedOccurrences");
    }

    @Test
    void coOccurrenceLookbacksShorterThanOneDay_areRejected() {
        RecommendationProperties properties = new RecommendationProperties();
        properties.setCoViewedLookback(Duration.ofHours(12));
        properties.setCoPurchasedLookback(Duration.ZERO);

        assertThat(validator.validate(properties))
                .extracting(violation -> violation.getMessage())
                .anyMatch(message -> message.contains("Co-viewed lookback"))
                .anyMatch(message -> message.contains("Co-purchased lookback"));
    }
}
