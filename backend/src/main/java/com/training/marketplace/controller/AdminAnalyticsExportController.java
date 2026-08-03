package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.CreateAnalyticsExportRequest;
import com.training.marketplace.dto.response.AnalyticsExportJobResponse;
import com.training.marketplace.service.AnalyticsExportService;
import com.training.marketplace.service.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/analytics/exports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Analytics Exports", description = "Asynchronous aggregate CSV exports")
public class AdminAnalyticsExportController {

    private static final MediaType CSV_MEDIA_TYPE = MediaType.parseMediaType("text/csv");

    private final AnalyticsExportService analyticsExportService;
    private final AuditService auditService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Create an asynchronous aggregate analytics CSV export")
    public ApiResponse<AnalyticsExportJobResponse> create(
            @Valid @RequestBody CreateAnalyticsExportRequest request,
            Authentication authentication) {
        AnalyticsExportJobResponse response = analyticsExportService.create(request);
        auditService.record(actor(authentication), "ANALYTICS_EXPORT_CREATE",
                "AnalyticsExportJob", response.id(), "type=" + response.type());
        return ApiResponse.success("Analytics export queued", response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get analytics export status")
    public ApiResponse<AnalyticsExportJobResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(analyticsExportService.get(id));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a completed analytics CSV export")
    public ResponseEntity<byte[]> download(@PathVariable UUID id, Authentication authentication) {
        AnalyticsExportService.AnalyticsExportDownload download = analyticsExportService.download(id);
        auditService.record(actor(authentication), "ANALYTICS_EXPORT_DOWNLOAD",
                "AnalyticsExportJob", id, "fileName=" + download.fileName());
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(CSV_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.content());
    }

    private String actor(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }
}
