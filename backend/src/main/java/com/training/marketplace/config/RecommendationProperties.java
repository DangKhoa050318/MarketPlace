package com.training.marketplace.config;

import com.training.marketplace.enums.OrderStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Set;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "marketplace.recommendation")
public class RecommendationProperties {

    private static final Set<OrderStatus> ALLOWED_BEST_SELLER_STATUSES = EnumSet.of(
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING,
            OrderStatus.SHIPPED,
            OrderStatus.DELIVERED);

    @NotNull
    private Duration bestSellerLookback = Duration.ofDays(30);

    @NotEmpty
    private Set<OrderStatus> validOrderStatuses =
            EnumSet.copyOf(ALLOWED_BEST_SELLER_STATUSES);

    @NotNull
    private Duration coViewedLookback = Duration.ofDays(90);

    @Min(2)
    private int minCoViewedOccurrences = 2;

    @NotNull
    private Duration coPurchasedLookback = Duration.ofDays(90);

    @Min(2)
    private int minCoPurchasedOccurrences = 2;

    @Min(50)
    private int similarCandidatePoolSize = 500;

    @Min(24)
    private int similarPrecomputeSize = 50;

    @AssertTrue(message = "Best-seller valid statuses cannot include PENDING or CANCELLED")
    public boolean isValidOrderStatusConfiguration() {
        return validOrderStatuses != null
                && !validOrderStatuses.isEmpty()
                && ALLOWED_BEST_SELLER_STATUSES.containsAll(validOrderStatuses);
    }

    @AssertTrue(message = "Best-seller lookback must be at least one day")
    public boolean isValidBestSellerLookback() {
        return bestSellerLookback != null
                && bestSellerLookback.compareTo(Duration.ofDays(1)) >= 0;
    }

    @AssertTrue(message = "Co-viewed lookback must be at least one day")
    public boolean isValidCoViewedLookback() {
        return coViewedLookback != null
                && coViewedLookback.compareTo(Duration.ofDays(1)) >= 0;
    }

    @AssertTrue(message = "Co-purchased lookback must be at least one day")
    public boolean isValidCoPurchasedLookback() {
        return coPurchasedLookback != null
                && coPurchasedLookback.compareTo(Duration.ofDays(1)) >= 0;
    }

    public Set<OrderStatus> validOrderStatusesSnapshot() {
        return validOrderStatuses == null || validOrderStatuses.isEmpty()
                ? Set.of()
                : Set.copyOf(validOrderStatuses);
    }
}
