package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.UpdateModerationStatusRequest;
import com.training.marketplace.dto.response.ModerationAuditLogResponse;
import com.training.marketplace.dto.response.ModerationItemResponse;
import com.training.marketplace.enums.ContentType;
import com.training.marketplace.enums.ModerationStatus;
import com.training.marketplace.service.ContentModerationService;
import com.training.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/moderation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
@Tag(name = "Content Moderation", description = "Admin content moderation queue and workflow")
public class ModerationController {

    private final ContentModerationService moderationService;
    private final UserService userService;

    @GetMapping("/queue")
    @Operation(summary = "Get moderation queue with filters (STAFF/MANAGER/ADMIN)")
    public ApiResponse<Page<ModerationItemResponse>> getQueue(
            @RequestParam(required = false) ContentType targetType,
            @RequestParam(required = false) ModerationStatus status,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ModerationItemResponse> queue = moderationService.getModerationQueue(targetType, status, productId, startDate, endDate, pageable);
        return ApiResponse.success(queue);
    }

    @PutMapping("/status")
    @Operation(summary = "Update content moderation status with mandatory reason when hiding/rejecting")
    public ApiResponse<ModerationItemResponse> updateStatus(
            @Valid @RequestBody UpdateModerationStatusRequest request,
            Authentication authentication) {

        Long moderatorId = userService.getAuthenticatedUser(authentication).getId();
        ModerationItemResponse response = moderationService.updateStatus(moderatorId, request);
        return ApiResponse.success("Moderation status updated", response);
    }

    @GetMapping("/logs")
    @Operation(summary = "Get audit logs for a target content item")
    public ApiResponse<List<ModerationAuditLogResponse>> getAuditLogs(
            @RequestParam ContentType targetType,
            @RequestParam Long targetId) {

        List<ModerationAuditLogResponse> logs = moderationService.getAuditLogs(targetType, targetId);
        return ApiResponse.success(logs);
    }
}

