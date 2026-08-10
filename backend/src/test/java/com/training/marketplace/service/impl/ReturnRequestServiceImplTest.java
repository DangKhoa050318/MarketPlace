package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.ReturnQcRequest;
import com.training.marketplace.dto.response.ReturnRequestResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.RefundRequest;
import com.training.marketplace.entity.ReturnRequest;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.enums.RefundRequestStatus;
import com.training.marketplace.enums.ReturnRequestStatus;
import com.training.marketplace.exception.RefundProcessingException;
import com.training.marketplace.repository.DeliveryRepository;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.RefundRequestRepository;
import com.training.marketplace.repository.ReturnRequestRepository;
import com.training.marketplace.service.InventoryFacade;
import com.training.marketplace.service.PaygateClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReturnRequestServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private RefundRequestRepository refundRequestRepository;

    @Mock
    private PaygateClientService paygateClientService;

    @Mock
    private InventoryFacade inventoryFacade;

    private ReturnRequestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ReturnRequestServiceImpl(
                orderRepository,
                deliveryRepository,
                returnRequestRepository,
                refundRequestRepository,
                paygateClientService,
                inventoryFacade);
    }

    @Test
    void recordQc_refundFails_marksQcPassedAndThrowsWithoutRestocking() {
        ReturnRequest rr = approvedWholeOrderReturn();
        Order order = paidReturnedOrder();
        when(returnRequestRepository.findById(10L)).thenReturn(Optional.of(rr));
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(refundRequestRepository.findByIdempotencyKey("PAYGATE_REFUND:RETURN:10:FULL"))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(returnRequestRepository.save(rr)).thenReturn(rr);
        doThrow(new IllegalStateException("Merchant balance is insufficient"))
                .when(paygateClientService)
                .refund(eq("TXN-100"), eq(100L), eq(new BigDecimal("200.00")), eq("PAYGATE_REFUND:RETURN:10:FULL"));

        assertThatThrownBy(() -> service.recordQc(10L, new ReturnQcRequest(true, "QC passed")))
                .isInstanceOf(RefundProcessingException.class)
                .hasMessageContaining("refund could not be processed");

        assertThat(rr.getStatus()).isEqualTo(ReturnRequestStatus.QC_PASSED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
        verify(returnRequestRepository).save(rr);
        verify(inventoryFacade, never()).restockReturn(any(), any());
    }

    @Test
    void recordQc_refundSucceeds_restocksReturnedItemsAndCompletesReturn() {
        ReturnRequest rr = approvedWholeOrderReturn();
        Order order = paidReturnedOrder();
        when(returnRequestRepository.findById(10L)).thenReturn(Optional.of(rr));
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(refundRequestRepository.findByIdempotencyKey("PAYGATE_REFUND:RETURN:10:FULL"))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(order)).thenReturn(order);
        when(returnRequestRepository.save(rr)).thenReturn(rr);

        ReturnRequestResponse response = service.recordQc(10L, new ReturnQcRequest(true, "QC passed"));

        assertThat(response.status()).isEqualTo(ReturnRequestStatus.COMPLETED);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(inventoryFacade).restockReturn(7L, Map.of(501L, 2));
    }

    @Test
    void partialRefund_usesDiscountedItemAmountAndCompletesOnlyAfterRefundSuccess() {
        Order order = paidReturnedOrder();
        order.setDiscountAmount(new BigDecimal("20.00"));
        order.setTotalAmount(new BigDecimal("180.00"));
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(returnRequestRepository.save(any(ReturnRequest.class)))
                .thenAnswer(invocation -> savedReturnRequest(invocation.getArgument(0), 20L));
        when(refundRequestRepository.findByIdempotencyKey("PAYGATE_REFUND:PARTIAL:20"))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(refundRequestRepository.sumAmountByOrderIdAndStatus(100L, RefundRequestStatus.SUCCEEDED))
                .thenReturn(BigDecimal.ZERO);

        ReturnRequestResponse response = service.partialRefund(100L, new com.training.marketplace.dto.request.PartialRefundRequest(
                500L, 1, "One defective unit"));

        assertThat(response.status()).isEqualTo(ReturnRequestStatus.COMPLETED);
        assertThat(order.getItems().get(0).getRefundedQuantity()).isEqualTo(1);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_REFUNDED);
        verify(paygateClientService).refund("TXN-100", 100L, new BigDecimal("90.00"), "PAYGATE_REFUND:PARTIAL:20");
    }

    @Test
    void partialRefund_refundFailsLeavesRequestApprovedAndThrowsWithoutMarkingQuantityRefunded() {
        Order order = paidReturnedOrder();
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(returnRequestRepository.save(any(ReturnRequest.class)))
                .thenAnswer(invocation -> savedReturnRequest(invocation.getArgument(0), 21L));
        when(refundRequestRepository.findByIdempotencyKey("PAYGATE_REFUND:PARTIAL:21"))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        doThrow(new IllegalStateException("Merchant balance is insufficient"))
                .when(paygateClientService)
                .refund(eq("TXN-100"), eq(100L), eq(new BigDecimal("100.00")), eq("PAYGATE_REFUND:PARTIAL:21"));

        assertThatThrownBy(() -> service.partialRefund(100L, new com.training.marketplace.dto.request.PartialRefundRequest(
                500L, 1, "One defective unit")))
                .isInstanceOf(RefundProcessingException.class)
                .hasMessageContaining("Partial refund request was saved");

        assertThat(order.getItems().get(0).getRefundedQuantity()).isZero();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_PENDING);
    }

    @Test
    void partialRefund_repeatedSameQuantityUsesNewRefundRequestKeyAndDoesNotReuseOldRefund() {
        Order order = paidReturnedOrder();
        order.getItems().get(0).setRefundedQuantity(1);
        order.setPaymentStatus(PaymentStatus.PARTIALLY_REFUNDED);
        when(orderRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(order));
        when(returnRequestRepository.save(any(ReturnRequest.class)))
                .thenAnswer(invocation -> savedReturnRequest(invocation.getArgument(0), 22L));
        when(refundRequestRepository.findByIdempotencyKey("PAYGATE_REFUND:PARTIAL:22"))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ReturnRequestResponse response = service.partialRefund(100L, new com.training.marketplace.dto.request.PartialRefundRequest(
                500L, 1, "Second defective unit"));

        assertThat(response.status()).isEqualTo(ReturnRequestStatus.COMPLETED);
        assertThat(order.getItems().get(0).getRefundedQuantity()).isEqualTo(2);
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paygateClientService).refund("TXN-100", 100L, new BigDecimal("100.00"), "PAYGATE_REFUND:PARTIAL:22");
    }

    private ReturnRequest approvedWholeOrderReturn() {
        ReturnRequest rr = ReturnRequest.builder()
                .orderId(100L)
                .userId(1L)
                .status(ReturnRequestStatus.APPROVED)
                .reason("Item is defective")
                .build();
        rr.setId(10L);
        return rr;
    }

    private ReturnRequest savedReturnRequest(ReturnRequest request, Long id) {
        request.setId(id);
        return request;
    }

    private Order paidReturnedOrder() {
        Order order = Order.builder()
                .user(User.builder().build())
                .warehouseId(7L)
                .status(OrderStatus.DELIVERED)
                .paymentStatus(PaymentStatus.PAID)
                .paygateTransactionRef("TXN-100")
                .totalAmount(new BigDecimal("200.00"))
                .shippingFee(BigDecimal.ZERO)
                .shippingAddress("123 Street")
                .build();
        order.setId(100L);
        order.getUser().setId(1L);
        OrderItem item = OrderItem.builder()
                .id(500L)
                .variantId(501L)
                .productName("Product")
                .variantName("Default")
                .sku("SKU-1")
                .quantity(2)
                .refundedQuantity(0)
                .unitPrice(new BigDecimal("100.00"))
                .subtotal(new BigDecimal("200.00"))
                .build();
        order.addItem(item);
        return order;
    }
}
