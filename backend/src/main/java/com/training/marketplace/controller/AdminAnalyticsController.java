package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.AnalyticsRetentionRequest;
import com.training.marketplace.dto.response.AnalyticsRetentionResponse;
import com.training.marketplace.dto.response.FunnelSummaryResponse;
import com.training.marketplace.service.AnalyticsEventService;
import com.training.marketplace.service.FunnelAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/analytics")
@RequiredArgsConstructor
@Tag(name = "Admin Analytics", description = "Funnel summaries and privacy operations")
public class AdminAnalyticsController {

    private final FunnelAnalyticsService funnelAnalyticsService;
    private final AnalyticsEventService analyticsEventService;

    @GetMapping("/funnel")
    @Operation(summary = "Summarize storefront journey funnel")
    public ApiResponse<FunnelSummaryResponse> funnel(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String campaign,
            @RequestParam(required = false) String deviceType) {
        return ApiResponse.success(funnelAnalyticsService.summarize(
                from, to, categoryId, productId, campaign, deviceType));
    }

    @PostMapping("/retention/anonymize")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Anonymize raw analytics identity older than the configured retention window")
    public ApiResponse<AnalyticsRetentionResponse> anonymizeExpired(
            @Valid @RequestBody AnalyticsRetentionRequest request) {
        return ApiResponse.success("Expired analytics identity anonymized",
                analyticsEventService.anonymizeExpiredRawEvents(request));
    }
}
