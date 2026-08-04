package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.repository.AnalyticsExportJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalyticsExportRecoveryJob {

    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(10);

    private final AnalyticsExportJobRepository analyticsExportJobRepository;
    private final AnalyticsExportProcessor analyticsExportProcessor;

    @Scheduled(fixedDelayString = "${marketplace.analytics.export-recovery-interval:PT1M}")
    public void recoverAndRetry() {
        Instant now = Instant.now();
        int recovered = analyticsExportJobRepository.recoverStuck(
                now.minus(PROCESSING_TIMEOUT), now);
        if (recovered > 0) {
            log.warn("analytics_export_recovered_stuck count={}", recovered);
        }
        analyticsExportJobRepository
                .findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        AnalyticsExportStatus.PENDING, now)
                .forEach(job -> analyticsExportProcessor.process(job.getPublicId()));
    }
}
