package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.UpdateOrderStatusRequest;
import com.training.marketplace.dto.response.OrderResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.OrderMapper;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.ReturnRequestRepository;
import com.training.marketplace.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PromotionService promotionService;

    @Mock
    private InventoryFacade inventoryFacade;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private User sampleUser;
    private Order sampleOrder;
    private OrderResponse sampleOrderResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .username("sampleuser")
                .email("user@example.com")
                .password("encoded_pass")
                .build();
        sampleUser.setId(10L);

        sampleOrder = Order.builder()
                .user(sampleUser)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("150.00"))
                .shippingAddress("123 Street")
                .build();
        sampleOrder.setId(1L);

        sampleOrderResponse = new OrderResponse(
                1L, 10L, "sampleuser", "user@example.com", "123 Street", new BigDecimal("150.00"),
                BigDecimal.ZERO, BigDecimal.ZERO, null, OrderStatus.PENDING, null, Collections.emptyList(),
                LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("getAdminOrders: null status filters returns all orders")
    void getAdminOrders_nullStatus_returnsAllOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleOrder)));
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleOrderResponse);

        PageResponse<OrderResponse> result = orderService.getAdminOrders(null, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAll(pageable);
    }

    @Test
    @DisplayName("getAdminOrders: with status filters by status")
    void getAdminOrders_withStatus_returnsFilteredOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        when(orderRepository.findAllByStatus(OrderStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleOrder)));
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleOrderResponse);

        PageResponse<OrderResponse> result = orderService.getAdminOrders(OrderStatus.PENDING, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        verify(orderRepository).findAllByStatus(OrderStatus.PENDING, pageable);
    }

    @Test
    @DisplayName("updateOrderStatusByAdmin: valid transition updates status successfully")
    void updateOrderStatusByAdmin_validTransition_success() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, "Approved by admin");
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(sampleOrder);
        when(orderMapper.toResponse(sampleOrder)).thenReturn(sampleOrderResponse);

        OrderResponse result = orderService.updateOrderStatusByAdmin(1L, request);

        assertThat(result).isNotNull();
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(sampleOrder.getNote()).isEqualTo("Approved by admin");
        verify(orderRepository).save(sampleOrder);
    }

    @Test
    @DisplayName("updateOrderStatusByAdmin: invalid transition throws BadRequestException")
    void updateOrderStatusByAdmin_invalidTransition_throwsException() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.DELIVERED, null);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(sampleOrder));

        assertThatThrownBy(() -> orderService.updateOrderStatusByAdmin(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot transition order status");
    }

    @Test
    @DisplayName("updateOrderStatusByAdmin: order not found throws ResourceNotFoundException")
    void updateOrderStatusByAdmin_orderNotFound_throwsException() {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CONFIRMED, null);
        when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatusByAdmin(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateOrderStatusByAdmin: cancelling a reserved order releases the reservation")
    void updateOrderStatusByAdmin_cancel_releasesReservation() {
        Order order = reservedOrder(OrderStatus.PENDING);
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CANCELLED, "Cancelled by admin");
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(sampleOrderResponse);

        orderService.updateOrderStatusByAdmin(2L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryFacade).release(eq(5L), eq(Map.of(100L, 2, 200L, 3)));
    }

    @Test
    @DisplayName("updateOrderStatusByAdmin: re-cancelling an already CANCELLED order does not release again")
    void updateOrderStatusByAdmin_cancelAlreadyCancelled_noDoubleRelease() {
        Order order = reservedOrder(OrderStatus.CANCELLED);
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CANCELLED, null);
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        orderService.updateOrderStatusByAdmin(2L, request);

        verify(inventoryFacade, never()).release(any(), any());
    }

    @Test
    @DisplayName("updateOrderStatusByAdmin: shipped order must be completed through delivery tracking")
    void updateOrderStatusByAdmin_shippedToDelivered_requiresDeliveryTracking() {
        Order order = reservedOrder(OrderStatus.SHIPPED);
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.DELIVERED, null);
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatusByAdmin(2L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("delivery tracking");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("updateOrderStatusByAdmin: cancelling a SHIPPED order (failed delivery) returns stock on-hand")
    void updateOrderStatusByAdmin_cancelShipped_returnsStock() {
        Order order = reservedOrder(OrderStatus.SHIPPED);
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.CANCELLED, "Failed delivery / boomed");
        when(orderRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(sampleOrderResponse);

        orderService.updateOrderStatusByAdmin(2L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        // Shipped stock was already decremented at ship → add it back on-hand, not a reservation release.
        verify(inventoryFacade).returnStock(eq(5L), eq(Map.of(100L, 2, 200L, 3)));
        verify(inventoryFacade, never()).release(any(), any());
    }

    /** An order with a warehouse and two reserved variants (100 -> 2, 200 -> 3). */
    private Order reservedOrder(OrderStatus status) {
        Order order = Order.builder()
                .user(sampleUser)
                .warehouseId(5L)
                .status(status)
                .totalAmount(new BigDecimal("250.00"))
                .shippingAddress("123 Street")
                .build();
        order.setId(2L);
        order.addItem(OrderItem.builder()
                .variantId(100L).sku("SKU-1").productName("P1").variantName("V1")
                .unitPrice(new BigDecimal("50.00")).quantity(2).subtotal(new BigDecimal("100.00"))
                .build());
        order.addItem(OrderItem.builder()
                .variantId(200L).sku("SKU-2").productName("P2").variantName("V2")
                .unitPrice(new BigDecimal("50.00")).quantity(3).subtotal(new BigDecimal("150.00"))
                .build());
        return order;
    }
}
