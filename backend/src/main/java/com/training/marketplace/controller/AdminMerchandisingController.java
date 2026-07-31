package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.response.MerchandisingSummaryResponse;
import com.training.marketplace.enums.MerchandisingTargetType;
import com.training.marketplace.service.MerchandisingEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/** Merchandising effectiveness reporting (ADMIN, B-408). */
@RestController
@RequestMapping("/api/v1/admin/merchandising")
@RequiredArgsConstructor
@Tag(name = "Admin Merchandising", description = "Merchandising effectiveness summary (ADMIN)")
public class AdminMerchandisingController {

    private final MerchandisingEventService merchandisingEventService;

    @GetMapping("/summary")
    @Operation(summary = "Effectiveness of a target over a time range: impressions, clicks, CTR, attributed orders")
    public ApiResponse<MerchandisingSummaryResponse> summary(
            @RequestParam MerchandisingTargetType targetType,
            @RequestParam Long targetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.success(merchandisingEventService.summary(targetType, targetId, from, to));
    }
}
