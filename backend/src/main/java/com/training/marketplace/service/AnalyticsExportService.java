package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateAnalyticsExportRequest;
import com.training.marketplace.dto.response.AnalyticsExportJobResponse;

import java.util.UUID;

public interface AnalyticsExportService {

    AnalyticsExportJobResponse create(CreateAnalyticsExportRequest request);

    AnalyticsExportJobResponse get(UUID id);

    AnalyticsExportDownload download(UUID id);

    record AnalyticsExportDownload(String fileName, byte[] content) {
    }
}
