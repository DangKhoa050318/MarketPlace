package com.training.marketplace.service;

import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.service.impl.FunnelAnalyticsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FunnelAnalyticsServiceTest {

    @Mock private AnalyticsEventRepository analyticsEventRepository;

    @InjectMocks private FunnelAnalyticsServiceImpl funnelAnalyticsService;

    @Test
    void summarize_calculatesConversionAndDropOffAcrossFunnelSteps() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-01T00:00:00Z");
        when(analyticsEventRepository.summarizeFunnel(from, to, 1L, 10L, "summer", "mobile"))
                .thenReturn(counts(100, 40, 20, 5));

        var result = funnelAnalyticsService.summarize(from, to, 1L, 10L, " summer ", " mobile ");

        assertThat(result.steps()).extracting("step")
                .containsExactly("PRODUCT_VIEW", "ADD_TO_CART", "BEGIN_CHECKOUT", "ORDER_CREATED");
        assertThat(result.steps()).extracting("count")
                .containsExactly(100L, 40L, 20L, 5L);
        assertThat(result.steps().get(1).conversionRate()).isEqualTo(0.4);
        assertThat(result.steps().get(1).dropOffRate()).isEqualTo(0.6);
        assertThat(result.steps().get(2).conversionRate()).isEqualTo(0.5);
        assertThat(result.steps().get(3).conversionRate()).isEqualTo(0.25);
    }

    @Test
    void summarize_zeroPreviousStepAvoidsDivideByZero() {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-01T00:00:00Z");
        when(analyticsEventRepository.summarizeFunnel(from, to, null, null, null, null))
                .thenReturn(counts(0, 0, 0, 0));

        var result = funnelAnalyticsService.summarize(from, to, null, null, null, null);

        assertThat(result.steps()).allSatisfy(step -> {
            assertThat(step.conversionRate()).isZero();
            assertThat(step.dropOffRate()).isZero();
        });
    }

    @Test
    void summarize_invalidDateRangeThrowsBadRequest() {
        Instant from = Instant.parse("2026-08-01T00:00:00Z");
        Instant to = Instant.parse("2026-07-01T00:00:00Z");

        assertThatThrownBy(() -> funnelAnalyticsService.summarize(from, to, null, null, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("from/to");
    }

    private AnalyticsEventRepository.FunnelCounts counts(
            long productViews,
            long addToCarts,
            long beginCheckouts,
            long orderCreated) {
        return new AnalyticsEventRepository.FunnelCounts() {
            @Override public long getProductViews() { return productViews; }
            @Override public long getAddToCarts() { return addToCarts; }
            @Override public long getBeginCheckouts() { return beginCheckouts; }
            @Override public long getOrderCreated() { return orderCreated; }
        };
    }
}
