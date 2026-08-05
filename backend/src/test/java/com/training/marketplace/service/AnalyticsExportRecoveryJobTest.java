package com.training.marketplace.service;

import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.entity.AnalyticsExportJob;
import com.training.marketplace.repository.AnalyticsExportJobRepository;
import com.training.marketplace.service.impl.AnalyticsExportProcessor;
import com.training.marketplace.service.impl.AnalyticsExportRecoveryJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsExportRecoveryJobTest {

    @Mock private AnalyticsExportJobRepository analyticsExportJobRepository;
    @Mock private AnalyticsExportProcessor analyticsExportProcessor;
    @InjectMocks private AnalyticsExportRecoveryJob analyticsExportRecoveryJob;

    @Test
    void recoverAndRetry_recoversStuckJobsAndDispatchesDueJobs() {
        UUID publicId = UUID.randomUUID();
        AnalyticsExportJob dueJob = AnalyticsExportJob.builder().publicId(publicId).build();
        when(analyticsExportJobRepository.recoverStuck(any(), any())).thenReturn(1);
        when(analyticsExportJobRepository.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                eq(AnalyticsExportStatus.PENDING), any())).thenReturn(List.of(dueJob));

        analyticsExportRecoveryJob.recoverAndRetry();

        verify(analyticsExportJobRepository).recoverStuck(any(), any());
        verify(analyticsExportProcessor).process(publicId);
    }
}
