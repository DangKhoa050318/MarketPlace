package com.training.marketplace.service;

import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.analytics.AnalyticsExportType;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.entity.AnalyticsExportJob;
import com.training.marketplace.repository.AnalyticsExportJobRepository;
import com.training.marketplace.service.impl.AnalyticsExportProcessor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsExportProcessorTest {

    @Mock private AnalyticsExportJobRepository analyticsExportJobRepository;
    @Mock private AnalyticsDashboardService analyticsDashboardService;

    @InjectMocks private AnalyticsExportProcessor analyticsExportProcessor;

    @Test
    void process_generatesAggregateOnlyOverviewCsv() {
        AnalyticsExportJob job = AnalyticsExportJob.builder()
                .publicId(UUID.randomUUID())
                .exportType(AnalyticsExportType.OVERVIEW)
                .status(AnalyticsExportStatus.PENDING)
                .attemptCount(1)
                .from(Instant.parse("2026-07-01T00:00:00Z"))
                .to(Instant.parse("2026-07-31T00:00:00Z"))
                .build();
        when(analyticsExportJobRepository.claim(any(), any())).thenReturn(1);
        when(analyticsExportJobRepository.findByPublicId(job.getPublicId()))
                .thenReturn(Optional.of(job));
        when(analyticsDashboardService.overview(any()))
                .thenReturn(new AnalyticsOverviewResponse(
                        100,
                        20,
                        10,
                        5,
                        new BigDecimal("0.2000"),
                        new BigDecimal("0.5000"),
                        new BigDecimal("0.0500"),
                        new BigDecimal("0.3000"),
                        Instant.parse("2026-07-31T00:00:00Z")));

        analyticsExportProcessor.process(job.getPublicId());

        assertThat(job.getStatus()).isEqualTo(AnalyticsExportStatus.COMPLETED);
        assertThat(job.getCsvContent())
                .contains("productViews,addToCarts")
                .contains("100,20,10,5")
                .doesNotContain("userId", "sessionId", "email", "phone", "address");
        assertThat(job.getExpiresAt()).isAfter(job.getCompletedAt());
        verify(analyticsExportJobRepository).save(job);
    }

    @Test
    void process_schedulesRetryWhenGenerationFails() {
        AnalyticsExportJob job = failedJob(1);
        when(analyticsExportJobRepository.claim(any(), any())).thenReturn(1);
        when(analyticsExportJobRepository.findByPublicId(job.getPublicId())).thenReturn(Optional.of(job));
        when(analyticsDashboardService.overview(any())).thenThrow(new IllegalStateException("database unavailable"));

        analyticsExportProcessor.process(job.getPublicId());

        assertThat(job.getStatus()).isEqualTo(AnalyticsExportStatus.PENDING);
        assertThat(job.getNextAttemptAt()).isAfter(Instant.now());
        assertThat(job.getErrorMessage()).contains("database unavailable");
    }

    @Test
    void process_marksJobFailedAfterMaximumAttempts() {
        AnalyticsExportJob job = failedJob(3);
        when(analyticsExportJobRepository.claim(any(), any())).thenReturn(1);
        when(analyticsExportJobRepository.findByPublicId(job.getPublicId())).thenReturn(Optional.of(job));
        when(analyticsDashboardService.overview(any())).thenThrow(new IllegalStateException("database unavailable"));

        analyticsExportProcessor.process(job.getPublicId());

        assertThat(job.getStatus()).isEqualTo(AnalyticsExportStatus.FAILED);
        assertThat(job.getNextAttemptAt()).isNull();
    }

    private AnalyticsExportJob failedJob(int attemptCount) {
        return AnalyticsExportJob.builder()
                .publicId(UUID.randomUUID())
                .exportType(AnalyticsExportType.OVERVIEW)
                .status(AnalyticsExportStatus.PROCESSING)
                .attemptCount(attemptCount)
                .from(Instant.parse("2026-07-01T00:00:00Z"))
                .to(Instant.parse("2026-07-31T00:00:00Z"))
                .build();
    }
}
