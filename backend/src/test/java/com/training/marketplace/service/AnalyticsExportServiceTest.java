package com.training.marketplace.service;

import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.analytics.AnalyticsExportType;
import com.training.marketplace.dto.request.CreateAnalyticsExportRequest;
import com.training.marketplace.entity.AnalyticsExportJob;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.AnalyticsExportJobRepository;
import com.training.marketplace.service.impl.AnalyticsExportProcessor;
import com.training.marketplace.service.impl.AnalyticsExportServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsExportServiceTest {

    @Mock private AnalyticsExportJobRepository analyticsExportJobRepository;
    @Mock private AnalyticsExportProcessor analyticsExportProcessor;

    @InjectMocks private AnalyticsExportServiceImpl analyticsExportService;

    @Test
    void create_persistsPendingJobAndStartsProcessor() {
        when(analyticsExportJobRepository.saveAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = analyticsExportService.create(request(
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-31T00:00:00Z")));

        assertThat(response.status()).isEqualTo(AnalyticsExportStatus.PENDING);
        assertThat(response.downloadUrl()).isNull();
        verify(analyticsExportProcessor).process(response.id());
    }

    @Test
    void create_rejectsRangeLongerThanNinetyDays() {
        assertThatThrownBy(() -> analyticsExportService.create(request(
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-07-31T00:00:00Z"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("90 days");
    }

    @Test
    void download_returnsCompletedUnexpiredCsv() {
        AnalyticsExportJob job = AnalyticsExportJob.builder()
                .publicId(java.util.UUID.randomUUID())
                .exportType(AnalyticsExportType.OVERVIEW)
                .status(AnalyticsExportStatus.COMPLETED)
                .fileName("analytics.csv")
                .csvContent("productViews,orders\n10,2\n")
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(analyticsExportJobRepository.findByPublicId(job.getPublicId()))
                .thenReturn(Optional.of(job));

        var download = analyticsExportService.download(job.getPublicId());

        assertThat(download.fileName()).isEqualTo("analytics.csv");
        assertThat(new String(download.content(), java.nio.charset.StandardCharsets.UTF_8))
                .contains("productViews,orders")
                .doesNotContain("email", "sessionId", "userId");
    }

    private CreateAnalyticsExportRequest request(Instant from, Instant to) {
        return new CreateAnalyticsExportRequest(
                AnalyticsExportType.OVERVIEW,
                from,
                to,
                null,
                null,
                " summer ",
                null,
                "mobile");
    }
}
