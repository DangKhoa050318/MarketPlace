package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.analytics.AnalyticsIngestionStatus;
import com.training.marketplace.dto.request.TrackAnalyticsEventBatchRequest;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.dto.response.AnalyticsEventBatchResponse;
import com.training.marketplace.dto.response.AnalyticsEventResponse;
import com.training.marketplace.entity.AnalyticsEvent;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.service.AnalyticsEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsEventServiceImpl implements AnalyticsEventService {

    private static final String SCHEMA_VERSION = "v1";
    private static final int BATCH_LIMIT = 50;
    private static final int MAX_FUTURE_MINUTES = 5;
    private static final int MAX_AGE_DAYS = 7;

    private final AnalyticsEventRepository analyticsEventRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    @Transactional
    public AnalyticsEventResponse track(Long userId, String sessionId, TrackAnalyticsEventRequest request) {
        return process(userId, sessionId, request, LocalDateTime.now());
    }

    @Override
    @Transactional
    public AnalyticsEventBatchResponse trackBatch(Long userId, String sessionId, TrackAnalyticsEventBatchRequest request) {
        List<TrackAnalyticsEventRequest> events = request.events();
        if (events.size() > BATCH_LIMIT) {
            throw new IllegalArgumentException("Analytics event batch size cannot exceed " + BATCH_LIMIT);
        }

        LocalDateTime receivedAt = LocalDateTime.now();
        List<AnalyticsEventResponse> responses = new ArrayList<>(events.size());
        for (TrackAnalyticsEventRequest event : events) {
            responses.add(process(userId, sessionId, event, receivedAt));
        }

        int accepted = countStatus(responses, AnalyticsIngestionStatus.ACCEPTED);
        int duplicateIgnored = countStatus(responses, AnalyticsIngestionStatus.DUPLICATE_IGNORED);
        int rejected = countStatus(responses, AnalyticsIngestionStatus.REJECTED);
        return new AnalyticsEventBatchResponse(accepted, duplicateIgnored, rejected, responses);
    }

    private AnalyticsEventResponse process(
            Long userId,
            String sessionId,
            TrackAnalyticsEventRequest request,
            LocalDateTime receivedAt) {
        String validationError = validate(request, receivedAt);
        if (validationError != null) {
            return response(request.eventId(), request.type(), AnalyticsIngestionStatus.REJECTED, validationError, receivedAt);
        }

        if (analyticsEventRepository.existsByEventId(request.eventId())) {
            return response(request.eventId(), request.type(), AnalyticsIngestionStatus.DUPLICATE_IGNORED,
                    "Duplicate event ignored", receivedAt);
        }

        try {
            analyticsEventRepository.save(toEntity(userId, sessionId, request, receivedAt));
            log.info("Analytics event accepted: eventId={}, type={}", request.eventId(), request.type());
            return response(request.eventId(), request.type(), AnalyticsIngestionStatus.ACCEPTED,
                    "Accepted", receivedAt);
        } catch (DataIntegrityViolationException ex) {
            return response(request.eventId(), request.type(), AnalyticsIngestionStatus.DUPLICATE_IGNORED,
                    "Duplicate event ignored", receivedAt);
        }
    }

    private String validate(TrackAnalyticsEventRequest request, LocalDateTime receivedAt) {
        if (!StringUtils.hasText(request.eventId())) {
            return "eventId is required";
        }
        if (request.eventId().length() > 64) {
            return "eventId cannot exceed 64 characters";
        }
        if (!SCHEMA_VERSION.equals(request.schemaVersion())) {
            return "schemaVersion must be v1";
        }
        if (request.type() == null) {
            return "type is required";
        }
        if (request.occurredAt() == null) {
            return "occurredAt is required";
        }
        if (request.occurredAt().isAfter(receivedAt.plusMinutes(MAX_FUTURE_MINUTES))) {
            return "occurredAt cannot be more than 5 minutes in the future";
        }
        if (request.occurredAt().isBefore(receivedAt.minusDays(MAX_AGE_DAYS))) {
            return "occurredAt cannot be older than 7 days";
        }

        return validateByType(request);
    }

    private String validateByType(TrackAnalyticsEventRequest request) {
        return switch (request.type()) {
            case PAGE_VIEW -> requireText(request.path(), "path is required for PAGE_VIEW");
            case PRODUCT_VIEW, ADD_TO_WISHLIST -> validateProductEvent(request);
            case SEARCH -> requireText(request.query(), "query is required for SEARCH");
            case ADD_TO_CART -> validateAddToCart(request);
            case BEGIN_CHECKOUT -> null;
            case ORDER_CREATED -> requirePositive(request.properties(), "orderId", "orderId is required for ORDER_CREATED");
        };
    }

    private String validateProductEvent(TrackAnalyticsEventRequest request) {
        if (request.productId() == null || request.productId() <= 0) {
            return "productId is required";
        }
        if (!productRepository.existsById(request.productId())) {
            return "productId does not exist";
        }
        return null;
    }

    private String validateAddToCart(TrackAnalyticsEventRequest request) {
        if (request.variantId() == null || request.variantId() <= 0) {
            return "variantId is required for ADD_TO_CART";
        }
        if (!productVariantRepository.existsById(request.variantId())) {
            return "variantId does not exist";
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            return "quantity must be positive for ADD_TO_CART";
        }
        return null;
    }

    private String requireText(String value, String message) {
        return StringUtils.hasText(value) ? null : message;
    }

    private String requirePositive(Map<String, Object> properties, String key, String message) {
        Object value = properties != null ? properties.get(key) : null;
        if (value instanceof Number number && number.longValue() > 0) {
            return null;
        }
        return message;
    }

    private AnalyticsEvent toEntity(
            Long userId,
            String sessionId,
            TrackAnalyticsEventRequest request,
            LocalDateTime receivedAt) {
        return AnalyticsEvent.builder()
                .eventId(request.eventId().trim())
                .schemaVersion(request.schemaVersion())
                .eventType(request.type())
                .userId(userId)
                .sessionId(StringUtils.hasText(sessionId) ? sessionId.trim() : null)
                .source(trimToNull(request.source()))
                .deviceType(trimToNull(request.deviceType()))
                .campaign(trimToNull(request.campaign()))
                .productId(request.productId())
                .variantId(request.variantId())
                .quantity(request.quantity())
                .path(trimToNull(request.path()))
                .searchQuery(trimToNull(request.query()))
                .properties(request.properties() != null ? request.properties() : Map.of())
                .occurredAt(request.occurredAt())
                .receivedAt(receivedAt)
                .build();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private AnalyticsEventResponse response(
            String eventId,
            AnalyticsEventType type,
            AnalyticsIngestionStatus status,
            String message,
            LocalDateTime receivedAt) {
        return new AnalyticsEventResponse(eventId, type, status, message, receivedAt);
    }

    private int countStatus(List<AnalyticsEventResponse> responses, AnalyticsIngestionStatus status) {
        return (int) responses.stream()
                .filter(response -> response.status() == status)
                .count();
    }
}
