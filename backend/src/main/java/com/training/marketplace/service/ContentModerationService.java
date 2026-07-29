package com.training.marketplace.service;

import com.training.marketplace.dto.request.UpdateModerationStatusRequest;
import com.training.marketplace.dto.response.ModerationAuditLogResponse;
import com.training.marketplace.dto.response.ModerationItemResponse;
import com.training.marketplace.enums.ContentType;
import com.training.marketplace.enums.ModerationStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ContentModerationService {

    Page<ModerationItemResponse> getModerationQueue(
            ContentType targetType,
            ModerationStatus status,
            Long productId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    ModerationItemResponse updateStatus(Long moderatorId, UpdateModerationStatusRequest request);

    List<ModerationAuditLogResponse> getAuditLogs(ContentType targetType, Long targetId);
}
