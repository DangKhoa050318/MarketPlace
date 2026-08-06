package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.AddDeliveryEventRequest;
import com.training.marketplace.dto.request.CreateDeliveryRequest;
import com.training.marketplace.dto.request.UpdateDeliveryRequest;
import com.training.marketplace.dto.request.UpdateDeliveryStatusRequest;
import com.training.marketplace.dto.response.DeliveryEventResponse;
import com.training.marketplace.dto.response.DeliveryResponse;
import com.training.marketplace.entity.Delivery;
import com.training.marketplace.entity.DeliveryEvent;
import com.training.marketplace.entity.Order;
import com.training.marketplace.enums.DeliveryEventType;
import com.training.marketplace.enums.DeliveryStatus;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ConflictException;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.DeliveryEventRepository;
import com.training.marketplace.repository.DeliveryRepository;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryEventRepository deliveryEventRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public DeliveryResponse create(Long orderId, Long actorUserId, CreateDeliveryRequest request) {
        Order order = lockOrder(orderId);
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException("Delivery can only be created for a SHIPPED order");
        }
        if (deliveryRepository.findByOrderId(orderId).isPresent()) {
            throw new DuplicateResourceException("Delivery already exists for order " + orderId);
        }

        String carrier = normalizeCarrier(request.carrier());
        String trackingCode = normalizeTrackingCode(request.trackingCode());
        ensureTrackingAvailable(null, carrier, trackingCode);

        Delivery delivery = Delivery.builder()
                .orderId(orderId)
                .carrier(carrier)
                .trackingCode(trackingCode)
                .status(DeliveryStatus.PENDING)
                .estimatedDelivery(request.estimatedDelivery())
                .createdBy(actorUserId)
                .build();

        try {
            delivery = deliveryRepository.saveAndFlush(delivery);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("Delivery or carrier tracking code already exists");
        }

        appendEvent(delivery, actorUserId, UUID.randomUUID(),
                DeliveryEventType.DELIVERY_CREATED, null);
        return toResponse(delivery);
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResponse getForCustomer(Long orderId, Long customerUserId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        if (!order.getUser().getId().equals(customerUserId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        return toResponse(findByOrderId(orderId));
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryResponse getForAdmin(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }
        return toResponse(findByOrderId(orderId));
    }

    @Override
    @Transactional
    public DeliveryResponse update(Long deliveryId, UpdateDeliveryRequest request) {
        LockedDelivery locked = lockOrderThenDelivery(deliveryId);
        Delivery delivery = locked.delivery();
        if (delivery.getStatus() != DeliveryStatus.PENDING) {
            throw new BadRequestException("Delivery details can only be updated while status is PENDING");
        }
        if (request.carrier() == null && request.trackingCode() == null && request.estimatedDelivery() == null) {
            throw new BadRequestException("At least one delivery field must be provided");
        }

        String carrier = request.carrier() == null
                ? delivery.getCarrier() : normalizeCarrier(request.carrier());
        String trackingCode = request.trackingCode() == null
                ? delivery.getTrackingCode() : normalizeTrackingCode(request.trackingCode());
        ensureTrackingAvailable(deliveryId, carrier, trackingCode);

        delivery.setCarrier(carrier);
        delivery.setTrackingCode(trackingCode);
        if (request.estimatedDelivery() != null) {
            delivery.setEstimatedDelivery(request.estimatedDelivery());
        }
        try {
            return toResponse(deliveryRepository.saveAndFlush(delivery));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException(
                    "Tracking code already exists for carrier " + carrier + ": " + trackingCode);
        }
    }

    @Override
    @Transactional
    public DeliveryResponse updateStatus(
            Long deliveryId, Long actorUserId, UpdateDeliveryStatusRequest request) {
        LockedDelivery locked = lockOrderThenDelivery(deliveryId);
        Order order = locked.order();
        Delivery delivery = locked.delivery();

        if (!delivery.getVersion().equals(request.expectedVersion())) {
            throw new ConflictException(
                    "Delivery was changed by another request; reload it before updating status");
        }
        if (delivery.getStatus() == request.status()) {
            return toResponse(delivery);
        }
        validateTransition(delivery.getStatus(), request.status());
        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BadRequestException("Delivery status can only change while its order is SHIPPED");
        }

        DeliveryEventType eventType = resolveStatusEvent(
                delivery.getStatus(), request.status(), request.eventType());
        delivery.setStatus(request.status());
        if (request.status() == DeliveryStatus.DELIVERED) {
            LocalDateTime deliveredAt = LocalDateTime.now();
            delivery.setDeliveredAt(deliveredAt);
            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(deliveredAt);
            if (order.getPaymentMethod() == PaymentMethod.COD) {
                order.setPaymentStatus(PaymentStatus.PAID);
            }
            orderRepository.save(order);
        }

        Delivery saved = deliveryRepository.saveAndFlush(delivery);
        appendEvent(saved, actorUserId, UUID.randomUUID(), eventType, request.note());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public DeliveryResponse addEvent(Long deliveryId, Long actorUserId, AddDeliveryEventRequest request) {
        LockedDelivery locked = lockOrderThenDelivery(deliveryId);
        Delivery delivery = locked.delivery();

        if (deliveryEventRepository.findByDeliveryIdAndRequestId(deliveryId, request.requestId()).isPresent()) {
            return toResponse(delivery);
        }
        validateInformationalEvent(delivery.getStatus(), request.eventType());
        appendEvent(delivery, actorUserId, request.requestId(), request.eventType(), request.note());
        return toResponse(delivery);
    }

    @Override
    @Transactional
    public void completeForCustomerConfirmation(Long orderId, Long actorUserId) {
        Delivery existing = deliveryRepository.findByOrderId(orderId).orElse(null);
        if (existing == null) {
            // Order shipped without a tracking record — allow the plain order confirmation.
            return;
        }
        Delivery delivery = deliveryRepository.findByIdForUpdate(existing.getId()).orElse(existing);
        if (delivery.getStatus() == DeliveryStatus.DELIVERED) {
            return; // Already delivered — idempotent.
        }
        if (delivery.getStatus() != DeliveryStatus.IN_TRANSIT) {
            throw new BadRequestException(
                    "Chưa thể xác nhận đã nhận hàng: đơn vận chuyển chưa được lấy hàng / đang giao.");
        }
        LocalDateTime now = LocalDateTime.now();
        delivery.setStatus(DeliveryStatus.DELIVERED);
        delivery.setDeliveredAt(now);
        deliveryRepository.saveAndFlush(delivery);
        appendEvent(delivery, actorUserId, UUID.randomUUID(),
                DeliveryEventType.DELIVERED, "Khách xác nhận đã nhận hàng");
    }

    private Order lockOrder(Long orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    private LockedDelivery lockOrderThenDelivery(Long deliveryId) {
        Long orderId = deliveryRepository.findOrderIdById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", deliveryId));
        Order order = lockOrder(orderId);
        Delivery delivery = deliveryRepository.findByIdForUpdate(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery", deliveryId));
        return new LockedDelivery(order, delivery);
    }

    private Delivery findByOrderId(Long orderId) {
        return deliveryRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery for order " + orderId));
    }

    private void ensureTrackingAvailable(Long currentDeliveryId, String carrier, String trackingCode) {
        deliveryRepository.findByNormalizedTracking(carrier, trackingCode)
                .filter(existing -> !existing.getId().equals(currentDeliveryId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Tracking code already exists for carrier " + carrier + ": " + trackingCode);
                });
    }

    private static void validateTransition(DeliveryStatus current, DeliveryStatus target) {
        boolean valid = switch (current) {
            case PENDING -> target == DeliveryStatus.IN_TRANSIT || target == DeliveryStatus.FAILED;
            case IN_TRANSIT -> target == DeliveryStatus.DELIVERED || target == DeliveryStatus.FAILED;
            case FAILED -> target == DeliveryStatus.IN_TRANSIT;
            case DELIVERED -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    "Cannot transition delivery status from " + current + " to " + target);
        }
    }

    private static DeliveryEventType resolveStatusEvent(
            DeliveryStatus current, DeliveryStatus target, DeliveryEventType requested) {
        DeliveryEventType defaultType = switch (target) {
            case IN_TRANSIT -> DeliveryEventType.IN_TRANSIT;
            case DELIVERED -> DeliveryEventType.DELIVERED;
            case FAILED -> DeliveryEventType.DELIVERY_FAILED;
            case PENDING -> throw new BadRequestException("Cannot transition delivery back to PENDING");
        };
        if (requested == null) {
            return defaultType;
        }

        boolean valid = switch (target) {
            case IN_TRANSIT -> requested == DeliveryEventType.IN_TRANSIT
                    || (current == DeliveryStatus.PENDING && requested == DeliveryEventType.PICKED_UP);
            case DELIVERED -> requested == DeliveryEventType.DELIVERED;
            case FAILED -> requested == DeliveryEventType.DELIVERY_FAILED;
            case PENDING -> false;
        };
        if (!valid) {
            throw new BadRequestException(
                    "Event type " + requested + " is not valid for delivery status " + target);
        }
        return requested;
    }

    private static void validateInformationalEvent(
            DeliveryStatus status, DeliveryEventType eventType) {
        boolean valid = (status == DeliveryStatus.PENDING && eventType == DeliveryEventType.READY_FOR_PICKUP)
                || (status == DeliveryStatus.IN_TRANSIT && eventType == DeliveryEventType.OUT_FOR_DELIVERY);
        if (!valid) {
            throw new BadRequestException(
                    "Event type " + eventType + " is not valid while delivery status is " + status);
        }
    }

    private void appendEvent(
            Delivery delivery,
            Long actorUserId,
            UUID requestId,
            DeliveryEventType type,
            String note) {
        deliveryEventRepository.save(DeliveryEvent.builder()
                .deliveryId(delivery.getId())
                .eventType(type)
                .status(delivery.getStatus())
                .note(normalizeNote(note))
                .recordedBy(actorUserId)
                .requestId(requestId)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private DeliveryResponse toResponse(Delivery delivery) {
        List<DeliveryEventResponse> events = deliveryEventRepository
                .findByDeliveryIdOrderByOccurredAtAscIdAsc(delivery.getId()).stream()
                .map(event -> new DeliveryEventResponse(
                        event.getId(), event.getRequestId(), event.getEventType(), event.getStatus(),
                        event.getNote(), event.getOccurredAt()))
                .toList();
        return new DeliveryResponse(
                delivery.getId(), delivery.getOrderId(), delivery.getVersion(),
                delivery.getCarrier(), delivery.getTrackingCode(),
                delivery.getStatus(), delivery.getEstimatedDelivery(), delivery.getDeliveredAt(), events,
                delivery.getCreatedAt(), delivery.getUpdatedAt());
    }

    private static String normalizeCarrier(String value) {
        return value.trim();
    }

    private static String normalizeTrackingCode(String value) {
        return value.trim().toUpperCase();
    }

    private static String normalizeNote(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record LockedDelivery(Order order, Delivery delivery) {
    }
}
