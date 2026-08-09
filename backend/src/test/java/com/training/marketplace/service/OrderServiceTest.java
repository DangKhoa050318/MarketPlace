package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateOrderRequest;
import com.training.marketplace.dto.response.CartItemResponse;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.dto.response.OrderResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.entity.RefundRequest;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.enums.RefundRequestStatus;
import com.training.marketplace.enums.Role;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.mapper.OrderMapper;
import com.training.marketplace.publisher.OrderEventPublisher;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.RefundRequestRepository;
import com.training.marketplace.repository.ReturnRequestRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;
    @Mock private InventoryFacade inventoryFacade;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderEventPublisher orderEventPublisher;
    @Mock private PromotionService promotionService;
    @Mock private PaygateClientService paygateClientService;
    @Mock private MerchandisingEventService merchandisingEventService;
    @Mock private DeliveryService deliveryService;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private ReturnRequestRepository returnRequestRepository;

    @InjectMocks private OrderServiceImpl orderService;

    private User testUser;
    private CartResponse cartResponse;
    private Order testOrder;
    private OrderResponse testOrderResponse;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .username("testuser").email("test@example.com").password("encoded_pass")
                .role(Role.CUSTOMER).active(true).build();
        testUser.setId(1L);

        CartItemResponse cartItem = new CartItemResponse(
                10L, 100L, "LAP-1", "Laptop", "Silver / 16GB",
                BigDecimal.valueOf(100.00), 2, BigDecimal.valueOf(200.00), null);
        cartResponse = new CartResponse(1L, List.of(cartItem), BigDecimal.valueOf(200.00), BigDecimal.ZERO, 2);

        testOrder = Order.builder()
                .user(testUser).warehouseId(1L).status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(200.00)).shippingAddress("123 Main St").note("Leave at door")
                .build();
        testOrder.setId(100L);

        testOrderResponse = new OrderResponse(
                100L, 1L, "testuser", "test@example.com", "123 Main St",
                BigDecimal.valueOf(200.00), BigDecimal.ZERO, BigDecimal.ZERO, null, OrderStatus.PENDING, null, List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("createOrder: valid cart reserves stock, clears cart, publishes event")
    void createOrder_validCart_success() {
        CreateOrderRequest request = new CreateOrderRequest("123 Main St", "Leave at door", null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCart(1L)).thenReturn(cartResponse);
        when(inventoryFacade.defaultWarehouseId()).thenReturn(1L);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);
        stubCurrentVariantPrice();

        OrderResponse response = orderService.createOrder(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        verify(inventoryFacade).reserve(eq(1L), eq(Map.of(10L, 2)));
        verify(cartService).clearCart(1L);
        verify(orderEventPublisher).publishOrderCreatedEvent(any());
    }

    @Test
    @DisplayName("createOrder: empty cart throws BadRequestException")
    void createOrder_emptyCart_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest("123 Main St", null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCart(1L)).thenReturn(new CartResponse(1L, List.of(), BigDecimal.ZERO, BigDecimal.ZERO, 0));

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cart is empty");
    }

    @Test
    @DisplayName("createOrder: insufficient stock propagates BadRequestException")
    void createOrder_insufficientStock_throwsException() {
        CreateOrderRequest request = new CreateOrderRequest("123 Main St", null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCart(1L)).thenReturn(cartResponse);
        when(inventoryFacade.defaultWarehouseId()).thenReturn(1L);
        stubCurrentVariantPrice();
        doThrow(new BadRequestException("Insufficient stock for variant 10"))
                .when(inventoryFacade).reserve(eq(1L), anyMap());

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("createOrder BNPL: saves customer-selected upfront and finance split")
    void createOrder_bnpl_savesCustomerSelectedSplit() {
        CreateOrderRequest request = new CreateOrderRequest(
                "123 Main St", null, null, PaymentMethod.PAYGATE_BNPL,
                new BigDecimal("0.00"), new BigDecimal("200.00"), 3);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCart(1L)).thenReturn(cartResponse);
        when(inventoryFacade.defaultWarehouseId()).thenReturn(1L);
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);
        stubCurrentVariantPrice();

        orderService.createOrder(1L, request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getUpfrontAmount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(saved.getFinanceAmount()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("createOrder: zero-total order auto CONFIRMED/PAID, fulfils stock, skips PayGate")
    void createOrder_zeroTotal_autoPaidAndFulfilled_skipsPaygate() {
        CreateOrderRequest request = new CreateOrderRequest(
                "123 Main St", null, "FREE100", PaymentMethod.PAYGATE_BNPL, null, null, 3);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCart(1L)).thenReturn(cartResponse);
        when(inventoryFacade.defaultWarehouseId()).thenReturn(1L);
        when(promotionService.consume(eq("FREE100"), eq(1L), any(CartResponse.class)))
                .thenReturn(new AppliedCoupon(500L, "FREE100", new BigDecimal("200.00")));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);
        when(orderMapper.toResponse(testOrder)).thenReturn(testOrderResponse);
        stubCurrentVariantPrice();

        orderService.createOrder(1L, request);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(captor.capture());
        Order saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(saved.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(inventoryFacade).fulfill(eq(1L), anyMap());
        verifyNoInteractions(paygateClientService);
    }

    @Test
    @DisplayName("createOrder: blocks checkout when a variant price changed")
    void createOrder_priceChanged_blocksAndAsksToReviewCart() {
        CreateOrderRequest request = new CreateOrderRequest("123 Main St", null, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(cartService.getCart(1L)).thenReturn(cartResponse);
        when(inventoryFacade.defaultWarehouseId()).thenReturn(1L);
        ProductVariant repriced = ProductVariant.builder()
                .productId(100L).sku("LAP-1").variantName("Silver / 16GB")
                .price(new BigDecimal("120.00")).build();
        repriced.setId(10L);
        when(variantRepository.findAllById(any())).thenReturn(List.of(repriced));

        assertThatThrownBy(() -> orderService.createOrder(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Giá đã thay đổi");

        verify(inventoryFacade, never()).reserve(any(), anyMap());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("cancelUserOrder: paid online before ship refunds via PayGate and releases reservation")
    void cancelUserOrder_paidOnlineBeforeShip_refundsAndReleasesReservation() {
        Order order = paidOnlineOrder("TXN_123");
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(refundRequestRepository.findByIdempotencyKey("PAYGATE_REFUND:ORDER:100:FULL"))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(testOrderResponse);

        orderService.cancelUserOrder(1L, 100L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paygateClientService).refund(
                "TXN_123",
                100L,
                BigDecimal.valueOf(200.00),
                "PAYGATE_REFUND:ORDER:100:FULL");
        verify(inventoryFacade).release(eq(1L), eq(Map.of(10L, 2)));
    }

    @Test
    @DisplayName("cancelUserOrder: paid online missing transactionRef still cancels and leaves refund pending")
    void cancelUserOrder_paidOnlineMissingTransactionRef_setsRefundPending() {
        Order order = paidOnlineOrder(null);
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order));
        when(refundRequestRepository.findByIdempotencyKey("PAYGATE_REFUND:ORDER:100:FULL"))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(testOrderResponse);

        orderService.cancelUserOrder(1L, 100L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        verify(paygateClientService, never()).refund(any(), any(), any(), any());
        verify(inventoryFacade).release(eq(1L), eq(Map.of(10L, 2)));
    }

    private Order paidOnlineOrder(String transactionRef) {
        Order order = Order.builder()
                .user(testUser)
                .warehouseId(1L)
                .status(OrderStatus.CONFIRMED)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .paymentStatus(PaymentStatus.PAID)
                .totalAmount(BigDecimal.valueOf(200.00))
                .shippingAddress("123 Main St")
                .build();
        order.setId(100L);
        order.setPaygateTransactionRef(transactionRef);
        order.addItem(OrderItem.builder()
                .variantId(10L)
                .productId(100L)
                .sku("LAP-1")
                .productName("Laptop")
                .variantName("Silver / 16GB")
                .unitPrice(BigDecimal.valueOf(100.00))
                .quantity(2)
                .subtotal(BigDecimal.valueOf(200.00))
                .build());
        return order;
    }

    private void stubCurrentVariantPrice() {
        ProductVariant variant = ProductVariant.builder()
                .productId(100L).sku("LAP-1").variantName("Silver / 16GB")
                .price(BigDecimal.valueOf(100.00)).build();
        variant.setId(10L);
        when(variantRepository.findAllById(any())).thenReturn(List.of(variant));
    }
}
