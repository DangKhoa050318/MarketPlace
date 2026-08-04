package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.dto.request.CreateAnalyticsExportRequest;
import com.training.marketplace.dto.response.AnalyticsExportJobResponse;
import com.training.marketplace.entity.AnalyticsExportJob;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.AnalyticsExportJobRepository;
import com.training.marketplace.service.AnalyticsExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalyticsExportServiceImpl implements AnalyticsExportService {

    private static final Duration MAX_EXPORT_RANGE = Duration.ofDays(90);

    private final AnalyticsExportJobRepository analyticsExportJobRepository;
    private final AnalyticsExportProcessor analyticsExportProcessor;

    @Override
    public AnalyticsExportJobResponse create(CreateAnalyticsExportRequest request) {
        validateRange(request.from(), request.to());
        AnalyticsExportJob job = AnalyticsExportJob.builder()
                .publicId(UUID.randomUUID())
                .exportType(request.type())
                .status(AnalyticsExportStatus.PENDING)
                .from(request.from())
                .to(request.to())
                .categoryId(request.categoryId())
                .productId(request.productId())
                .campaign(blankToNull(request.campaign()))
                .placement(blankToNull(request.placement()))
                .deviceType(blankToNull(request.deviceType()))
                .nextAttemptAt(Instant.now().plus(Duration.ofMinutes(10)))
                .build();
        AnalyticsExportJob saved = analyticsExportJobRepository.saveAndFlush(job);
        analyticsExportProcessor.process(saved.getPublicId());
        return toResponse(saved);
    }

    @Override
    public AnalyticsExportJobResponse get(UUID id) {
        return toResponse(find(id));
    }

    @Override
    public AnalyticsExportDownload download(UUID id) {
        AnalyticsExportJob job = find(id);
        if (job.getStatus() != AnalyticsExportStatus.COMPLETED || job.getCsvContent() == null) {
            throw new BadRequestException("Analytics export is not ready");
        }
        if (job.getExpiresAt() == null || !job.getExpiresAt().isAfter(Instant.now())) {
            throw new BadRequestException("Analytics export download has expired");
        }
        return new AnalyticsExportDownload(
                job.getFileName(),
                job.getCsvContent().getBytes(StandardCharsets.UTF_8));
    }

    private AnalyticsExportJob find(UUID id) {
        return analyticsExportJobRepository.findByPublicId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Analytics export not found: " + id));
    }

    private AnalyticsExportJobResponse toResponse(AnalyticsExportJob job) {
        String downloadUrl = job.getStatus() == AnalyticsExportStatus.COMPLETED
                ? "/api/v1/admin/analytics/exports/" + job.getPublicId() + "/download"
                : null;
        return new AnalyticsExportJobResponse(
                job.getPublicId(),
                job.getExportType(),
                job.getStatus(),
                job.getFileName(),
                downloadUrl,
                job.getErrorMessage(),
                job.getCompletedAt(),
                job.getExpiresAt());
    }

    private void validateRange(Instant from, Instant to) {
        if (from == null || to == null || !from.isBefore(to)) {
            throw new BadRequestException("A valid analytics export date range is required");
        }
        if (Duration.between(from, to).compareTo(MAX_EXPORT_RANGE) > 0) {
            throw new BadRequestException("Analytics export date range must not exceed 90 days");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static AnalyticsDashboardFilter filter(AnalyticsExportJob job) {
        return new AnalyticsDashboardFilter(
                job.getFrom(),
                job.getTo(),
                job.getCategoryId(),
                job.getProductId(),
                job.getCampaign(),
                job.getPlacement(),
                job.getDeviceType());
    }
}
