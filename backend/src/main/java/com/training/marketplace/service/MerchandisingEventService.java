package com.training.marketplace.service;

import com.training.marketplace.dto.request.RecordMerchandisingEventRequest;
import com.training.marketplace.dto.response.MerchandisingSummaryResponse;
import com.training.marketplace.enums.MerchandisingTargetType;

import java.time.LocalDateTime;

public interface MerchandisingEventService {

    /** Records an impression/click; a replayed {@code eventId} is an idempotent no-op (B-407). */
    void record(RecordMerchandisingEventRequest request, Long userId);

    /**
     * Best-effort last-click attribution: stamps the most recent unattributed click by this user
     * within the attribution window with the given order id (B-408). Never throws to the caller.
     */
    void attributeOrder(Long orderId, Long userId, LocalDateTime orderTime);

    /** Effectiveness of one target over [from, to): impressions, clicks, CTR, attributed orders (B-408). */
    MerchandisingSummaryResponse summary(MerchandisingTargetType targetType, Long targetId,
                                         LocalDateTime from, LocalDateTime to);
}
