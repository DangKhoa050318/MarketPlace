package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.AnalyticsIngestionStatus;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.dto.response.AnalyticsEventResponse;
import com.training.marketplace.entity.AnalyticsEvent;
import com.training.marketplace.repository.AnalyticsEventIngestionRepository;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.service.AnalyticsEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    private final AnalyticsEventRepository analyticsEventRepository;
    private final AnalyticsEventIngestionRepository analyticsEventIngestionRepository;
    private final AnalyticsEventValidator analyticsEventValidator;

    @Override
    @Transactional
    public AnalyticsEventResponse track(Long userId, String sessionId, TrackAnalyticsEventRequest request) {
        UUID eventId = request.eventId() != null ? request.eventId() : UUID.randomUUID();
        Instant eventInstant = request.occurredAt() != null ? request.occurredAt() : Instant.now();
        Instant receivedAt = Instant.now();
        Map<String, Object> properties = request.properties();
        Long productId = canonicalLong(request.productId(), properties, "productId");
        Long variantId = canonicalLong(request.variantId(), properties, "variantId");
        Integer quantity = canonicalInteger(request.quantity(), properties, "quantity");
        Long orderId = canonicalLong(request.orderId(), properties, "orderId");
        BigDecimal unitPrice = canonicalDecimal(request.unitPrice(), properties, "unitPrice");
        analyticsEventValidator.validate(request, productId, variantId, quantity, eventInstant);

        AnalyticsEvent event = AnalyticsEvent.builder()
                .eventId(eventId)
                .schemaVersion(request.schemaVersion())
                .eventType(request.type())
                .userId(userId)
                .sessionId(sessionId)
                .productId(productId)
                .variantId(variantId)
                .source(request.source())
                .placement(request.placement())
                .recommendationRequestId(request.recommendationRequestId())
                .strategy(request.strategy())
                .position(request.position())
                .quantity(quantity)
                .orderId(orderId)
                .unitPrice(unitPrice)
                .occurredAt(eventInstant)
                .receivedAt(receivedAt)
                .properties(properties)
                .build();
        boolean inserted = analyticsEventIngestionRepository.insertIfAbsent(event);
        if (!inserted) {
            AnalyticsEvent existing = analyticsEventRepository.findByEventId(eventId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Event ID conflict reported but existing event was not found: " + eventId));
            log.debug("Duplicate analytics event ignored: eventId={}, type={}",
                    eventId, existing.getEventType());
            return toResponse(existing, AnalyticsIngestionStatus.DUPLICATE_IGNORED);
        }

        AnalyticsEventResponse response = toResponse(event, AnalyticsIngestionStatus.ACCEPTED);

        log.info(
                "Analytics event accepted: eventId={}, schemaVersion={}, type={}, authenticated={}, hasSession={}, "
                        + "productId={}, variantId={}, source={}, placement={}, recommendationRequestId={}, "
                        + "strategy={}, position={}, occurredAt={}, receivedAt={}",
                eventId, request.schemaVersion(), response.type(), userId != null,
                sessionId != null && !sessionId.isBlank(), productId, variantId, request.source(),
                request.placement(), request.recommendationRequestId(), request.strategy(),
                request.position(), eventInstant, receivedAt);
        return response;
    }

    private AnalyticsEventResponse toResponse(
            AnalyticsEvent event,
            AnalyticsIngestionStatus status) {
        return new AnalyticsEventResponse(
                event.getEventType(),
                event.getUserId(),
                event.getSessionId(),
                LocalDateTime.ofInstant(event.getOccurredAt(), ZoneOffset.UTC),
                event.getProperties(),
                event.getEventId(),
                status,
                event.getReceivedAt());
    }

    private Long canonicalLong(Long structuredValue, Map<String, Object> properties, String key) {
        if (structuredValue != null) {
            return structuredValue;
        }
        Object value = properties.get(key);
        return value instanceof Number number ? number.longValue() : null;
    }

    private Integer canonicalInteger(
            Integer structuredValue,
            Map<String, Object> properties,
            String key) {
        if (structuredValue != null) {
            return structuredValue;
        }
        Object value = properties.get(key);
        return value instanceof Number number ? number.intValue() : null;
    }

    private BigDecimal canonicalDecimal(
            BigDecimal structuredValue,
            Map<String, Object> properties,
            String key) {
        if (structuredValue != null) {
            return structuredValue;
        }
        Object value = properties.get(key);
        return value instanceof Number number ? new BigDecimal(number.toString()) : null;
    }
}
