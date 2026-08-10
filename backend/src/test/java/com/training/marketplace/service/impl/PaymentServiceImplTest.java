package com.training.marketplace.service.impl;

import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.RefundRequest;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.enums.RefundRequestStatus;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.RefundRequestRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.InventoryFacade;
import com.training.marketplace.service.PaygateClientService;
import com.training.marketplace.service.PromotionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private RefundRequestRepository refundRequestRepository;
    @Mock private InventoryFacade inventoryFacade;
    @Mock private PromotionService promotionService;
    @Mock private PaygateClientService paygateClientService;
    @Mock private UserRepository userRepository;

    private PaymentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PaymentServiceImpl(
                orderRepository,
                refundRequestRepository,
                inventoryFacade,
                promotionService,
                paygateClientService,
                userRepository);
    }

    @Test
    void cancelPayment_paidBnplCreditsMarketplaceWalletInsteadOfPaygateRefund() {
        Order order = paidOrder(PaymentMethod.PAYGATE_BNPL);
        when(refundRequestRepository.findByIdempotencyKey("PAYGATE_BNPL_CREDIT:ORDER:100:FULL"))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelPayment(order, "Customer cancelled before shipment");

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(order.getUser().getWalletBalance()).isEqualByComparingTo(new BigDecimal("250.00"));
        verify(userRepository).save(order.getUser());
        verify(paygateClientService, never()).refund(any(), any(), any(), any());
    }

    @Test
    void cancelPayment_paidNonBnplKeepsRefundFlow() {
        Order order = paidOrder(PaymentMethod.CREDIT_CARD);
        when(refundRequestRepository.findByIdempotencyKey("PAYGATE_REFUND:ORDER:100:FULL"))
                .thenReturn(Optional.empty());
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.cancelPayment(order, "Customer cancelled before shipment");

        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(paygateClientService).refund(
                "TXN-BNPL-100",
                100L,
                new BigDecimal("200.00"),
                "PAYGATE_REFUND:ORDER:100:FULL");
    }

    private Order paidOrder(PaymentMethod method) {
        User user = User.builder()
                .username("customer")
                .email("customer@example.com")
                .password("encoded")
                .walletBalance(new BigDecimal("50.00"))
                .build();
        user.setId(1L);
        Order order = Order.builder()
                .user(user)
                .paymentMethod(method)
                .paymentStatus(PaymentStatus.PAID)
                .totalAmount(new BigDecimal("200.00"))
                .paygateTransactionRef("TXN-BNPL-100")
                .build();
        order.setId(100L);
        return order;
    }
}
