package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.RecordMerchandisingEventRequest;
import com.training.marketplace.dto.response.MerchandisingSummaryResponse;
import com.training.marketplace.entity.MerchandisingEvent;
import com.training.marketplace.enums.MerchandisingEventType;
import com.training.marketplace.enums.MerchandisingTargetType;
import com.training.marketplace.repository.MerchandisingEventRepository;
import com.training.marketplace.service.MerchandisingEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchandisingEventServiceImpl implements MerchandisingEventService {

    /** Last-click attribution window (B-408). Configurable candidate; kept as a constant for now. */
    private static final long ATTRIBUTION_WINDOW_HOURS = 24;

    private final MerchandisingEventRepository eventRepository;

    @Override
    public void record(RecordMerchandisingEventRequest request, Long userId) {
        // Not @Transactional so save() runs in its own transaction — a duplicate eventId then rolls
        // back only that insert and we swallow it, rather than poisoning an outer transaction.
        if (eventRepository.existsByEventId(request.eventId())) {
            return; // idempotent replay
        }
        MerchandisingEvent event = MerchandisingEvent.builder()
                .eventId(request.eventId())
                .eventType(request.eventType())
                .targetType(request.targetType())
                .targetId(request.targetId())
                .userId(userId)
                .sessionId(request.sessionId())
                .build(); // occurredAt/receivedAt set by @PrePersist
        try {
            eventRepository.save(event);
        } catch (DataIntegrityViolationException dup) {
            // Concurrent insert of the same eventId — idempotent no-op.
            log.debug("Duplicate merchandising eventId ignored: {}", request.eventId());
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void attributeOrder(Long orderId, Long userId, LocalDateTime orderTime) {
        if (userId == null || orderTime == null) {
            return;
        }
        LocalDateTime since = orderTime.minusHours(ATTRIBUTION_WINDOW_HOURS);
        List<MerchandisingEvent> clicks =
                eventRepository.findRecentUnattributedClicksByUser(userId, since, orderTime);
        if (clicks.isEmpty()) {
            return;
        }
        MerchandisingEvent lastClick = clicks.get(0); // most recent (query orders by occurredAt DESC)
        lastClick.setOrderId(orderId);
        eventRepository.save(lastClick);
        log.debug("Attributed order {} to {}#{} via click {}",
                orderId, lastClick.getTargetType(), lastClick.getTargetId(), lastClick.getEventId());
    }

    @Override
    @Transactional(readOnly = true)
    public MerchandisingSummaryResponse summary(MerchandisingTargetType targetType, Long targetId,
                                                LocalDateTime from, LocalDateTime to) {
        long impressions = eventRepository.countInRange(
                targetType, targetId, MerchandisingEventType.IMPRESSION, from, to);
        long clicks = eventRepository.countInRange(
                targetType, targetId, MerchandisingEventType.CLICK, from, to);
        long attributedOrders = eventRepository.countAttributedOrders(targetType, targetId, from, to);
        double ctr = impressions == 0 ? 0.0 : (double) clicks / impressions;
        return new MerchandisingSummaryResponse(targetType, targetId, impressions, clicks, ctr, attributedOrders);
    }
}
