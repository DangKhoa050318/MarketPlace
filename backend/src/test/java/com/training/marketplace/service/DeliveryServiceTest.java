package com.training.marketplace.service;

import com.training.marketplace.dto.request.AddDeliveryEventRequest;
import com.training.marketplace.dto.request.CreateDeliveryRequest;
import com.training.marketplace.dto.request.UpdateDeliveryRequest;
import com.training.marketplace.dto.request.UpdateDeliveryStatusRequest;
import com.training.marketplace.dto.response.DeliveryResponse;
import com.training.marketplace.entity.Delivery;
import com.training.marketplace.entity.DeliveryEvent;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.User;
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
import com.training.marketplace.service.impl.DeliveryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryServiceTest {

    @Mock private DeliveryRepository deliveryRepository;
    @Mock private DeliveryEventRepository deliveryEventRepository;
    @Mock private OrderRepository orderRepository;

    @InjectMocks private DeliveryServiceImpl deliveryService;

    private User customer;
    private Order shippedOrder;
    private Delivery pendingDelivery;

    @BeforeEach
    void setUp() {
        customer = User.builder()
                .username("customer")
                .email("customer@example.com")
                .password("encoded")
                .build();
        customer.setId(7L);

        shippedOrder = Order.builder()
                .user(customer)
                .warehouseId(1L)
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("100.00"))
                .shippingAddress("Bangkok")
                .build();
        shippedOrder.setId(10L);

        pendingDelivery = Delivery.builder()
                .id(20L)
                .orderId(10L)
                .carrier("GHN")
                .trackingCode("GHN-001")
                .status(DeliveryStatus.PENDING)
                .estimatedDelivery(LocalDate.now().plusDays(2))
                .createdBy(2L)
                .version(0L)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_shippedOrder_createsPendingDeliveryAndInitialEvent() {
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(shippedOrder));
        when(deliveryRepository.findByOrderId(10L)).thenReturn(Optional.empty());
        when(deliveryRepository.findByNormalizedTracking("GHN", "ABC-123"))
                .thenReturn(Optional.empty());
        when(deliveryRepository.saveAndFlush(any(Delivery.class))).thenAnswer(invocation -> {
            Delivery delivery = invocation.getArgument(0);
            delivery.setId(20L);
            delivery.setCreatedAt(LocalDateTime.now());
            delivery.setUpdatedAt(LocalDateTime.now());
            return delivery;
        });
        when(deliveryEventRepository.findByDeliveryIdOrderByOccurredAtAscIdAsc(20L))
                .thenReturn(List.of());

        DeliveryResponse result = deliveryService.create(10L, 2L,
                new CreateDeliveryRequest(" GHN ", "abc-123", LocalDate.now().plusDays(2)));

        assertThat(result.status()).isEqualTo(DeliveryStatus.PENDING);
        assertThat(result.carrier()).isEqualTo("GHN");
        assertThat(result.trackingCode()).isEqualTo("ABC-123");
        verify(deliveryEventRepository).save(any(DeliveryEvent.class));
    }

    @Test
    void create_nonShippedOrder_rejected() {
        shippedOrder.setStatus(OrderStatus.PROCESSING);
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(shippedOrder));

        assertThatThrownBy(() -> deliveryService.create(10L, 2L,
                new CreateDeliveryRequest("GHN", "ABC", LocalDate.now().plusDays(1))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("SHIPPED");
    }

    @Test
    void create_existingDelivery_rejectedAsDuplicate() {
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(shippedOrder));
        when(deliveryRepository.findByOrderId(10L)).thenReturn(Optional.of(pendingDelivery));

        assertThatThrownBy(() -> deliveryService.create(10L, 2L,
                new CreateDeliveryRequest("GHN", "ABC", LocalDate.now().plusDays(1))))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getForCustomer_otherUsersOrder_returnsNotFound() {
        when(orderRepository.findById(10L)).thenReturn(Optional.of(shippedOrder));

        assertThatThrownBy(() -> deliveryService.getForCustomer(10L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(deliveryRepository, never()).findByOrderId(any());
    }

    @Test
    void update_pendingDelivery_updatesNormalizedTrackingDetails() {
        prepareLockedDelivery(pendingDelivery);
        when(deliveryRepository.findByNormalizedTracking("GHTK", "NEW-001"))
                .thenReturn(Optional.empty());
        when(deliveryRepository.saveAndFlush(pendingDelivery)).thenReturn(pendingDelivery);
        when(deliveryEventRepository.findByDeliveryIdOrderByOccurredAtAscIdAsc(20L))
                .thenReturn(List.of());

        DeliveryResponse result = deliveryService.update(20L,
                new UpdateDeliveryRequest(" GHTK ", "new-001", LocalDate.now().plusDays(3)));

        assertThat(result.carrier()).isEqualTo("GHTK");
        assertThat(result.trackingCode()).isEqualTo("NEW-001");
    }

    @Test
    void update_inTransitDelivery_rejected() {
        pendingDelivery.setStatus(DeliveryStatus.IN_TRANSIT);
        prepareLockedDelivery(pendingDelivery);

        assertThatThrownBy(() -> deliveryService.update(20L,
                new UpdateDeliveryRequest("GHTK", null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void updateStatus_delivered_updatesOrderAndAppendsEventAtomically() {
        shippedOrder.setPaymentMethod(PaymentMethod.COD);
        shippedOrder.setPaymentStatus(PaymentStatus.UNPAID);
        pendingDelivery.setStatus(DeliveryStatus.IN_TRANSIT);
        prepareLockedDelivery(pendingDelivery);
        when(deliveryRepository.saveAndFlush(pendingDelivery)).thenReturn(pendingDelivery);
        when(deliveryEventRepository.findByDeliveryIdOrderByOccurredAtAscIdAsc(20L))
                .thenReturn(List.of());

        DeliveryResponse result = deliveryService.updateStatus(20L, 2L,
                new UpdateDeliveryStatusRequest(
                        0L, DeliveryStatus.DELIVERED,
                        DeliveryEventType.DELIVERED, "Handed to customer"));

        assertThat(result.status()).isEqualTo(DeliveryStatus.DELIVERED);
        assertThat(result.deliveredAt()).isNotNull();
        assertThat(shippedOrder.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(shippedOrder.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        verify(orderRepository).save(shippedOrder);
        verify(deliveryEventRepository).save(any(DeliveryEvent.class));
    }

    @Test
    void updateStatus_invalidTransition_rejected() {
        prepareLockedDelivery(pendingDelivery);

        assertThatThrownBy(() -> deliveryService.updateStatus(20L, 2L,
                new UpdateDeliveryStatusRequest(0L, DeliveryStatus.DELIVERED, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transition");
    }

    @Test
    void updateStatus_staleVersion_rejectedBeforeApplyingCommand() {
        pendingDelivery.setVersion(1L);
        prepareLockedDelivery(pendingDelivery);

        assertThatThrownBy(() -> deliveryService.updateStatus(20L, 2L,
                new UpdateDeliveryStatusRequest(0L, DeliveryStatus.IN_TRANSIT, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reload");

        assertThat(pendingDelivery.getStatus()).isEqualTo(DeliveryStatus.PENDING);
        verify(deliveryRepository, never()).saveAndFlush(any(Delivery.class));
        verify(deliveryEventRepository, never()).save(any(DeliveryEvent.class));
    }

    @Test
    void updateStatus_sameStatus_isIdempotentWithoutNewEvent() {
        prepareLockedDelivery(pendingDelivery);
        when(deliveryEventRepository.findByDeliveryIdOrderByOccurredAtAscIdAsc(20L))
                .thenReturn(List.of());

        DeliveryResponse result = deliveryService.updateStatus(20L, 2L,
                new UpdateDeliveryStatusRequest(0L, DeliveryStatus.PENDING, null, null));

        assertThat(result.status()).isEqualTo(DeliveryStatus.PENDING);
        verify(deliveryEventRepository, never()).save(any(DeliveryEvent.class));
    }

    @Test
    void addEvent_duplicateRequestId_isIdempotent() {
        UUID requestId = UUID.randomUUID();
        prepareLockedDelivery(pendingDelivery);
        when(deliveryEventRepository.findByDeliveryIdAndRequestId(20L, requestId))
                .thenReturn(Optional.of(DeliveryEvent.builder().requestId(requestId).build()));
        when(deliveryEventRepository.findByDeliveryIdOrderByOccurredAtAscIdAsc(20L))
                .thenReturn(List.of());

        DeliveryResponse result = deliveryService.addEvent(20L, 2L,
                new AddDeliveryEventRequest(requestId, DeliveryEventType.READY_FOR_PICKUP, null));

        assertThat(result.id()).isEqualTo(20L);
        verify(deliveryEventRepository, never()).save(any(DeliveryEvent.class));
    }

    private void prepareLockedDelivery(Delivery delivery) {
        when(deliveryRepository.findOrderIdById(20L)).thenReturn(Optional.of(10L));
        when(orderRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(shippedOrder));
        when(deliveryRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(delivery));
    }
}
