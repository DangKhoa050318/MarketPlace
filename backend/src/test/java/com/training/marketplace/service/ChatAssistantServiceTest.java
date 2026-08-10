package com.training.marketplace.service;

import com.training.marketplace.config.ChatAssistantProperties;
import com.training.marketplace.dto.request.ChatMessageRequest;
import com.training.marketplace.dto.request.CreateOrderRequest;
import com.training.marketplace.dto.response.OrderResponse;
import com.training.marketplace.dto.response.PaygatePayloadResponse;
import com.training.marketplace.enums.ChatIntent;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PaymentMethod;
import com.training.marketplace.enums.PaymentStatus;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ForbiddenException;
import com.training.marketplace.service.impl.ChatAssistantServiceImpl;
import com.training.marketplace.service.impl.RuleBasedChatIntentAnalyzer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatAssistantServiceTest {

    @Mock private ChatConversationStore conversationStore;
    @Mock private ChatAiGateway aiGateway;
    @Mock private CampaignOfferService campaignOfferService;
    @Mock private ChatProductDiscoveryService discoveryService;
    @Mock private AnalyticsEventService analyticsEventService;
    @Mock private OrderService orderService;

    private ChatAssistantService service;

    @BeforeEach
    void setUp() {
        ChatAssistantProperties properties = new ChatAssistantProperties();
        properties.setMaxProducts(6);
        properties.setMaxHistoryMessages(12);
        service = new ChatAssistantServiceImpl(
                properties,
                conversationStore,
                aiGateway,
                new RuleBasedChatIntentAnalyzer(),
                campaignOfferService,
                discoveryService,
                analyticsEventService,
                orderService);
    }

    @Test
    void returnsInStockProductWithEffectiveScopedVoucher() {
        when(conversationStore.find(any())).thenReturn(Optional.empty());
        when(aiGateway.analyze(any(), any(), any())).thenReturn(Optional.empty());
        ChatCampaignOffer offer = offer();
        when(campaignOfferService.findEffectiveOffers(null)).thenReturn(List.of(offer));
        when(discoveryService.discover(any(), eq(true), eq(null), eq("session-1")))
                .thenReturn(List.of(candidate()));

        var response = service.reply(
                null,
                "session-1",
                new ChatMessageRequest(
                        null,
                        "Laptop bán chạy dưới 20 triệu có voucher",
                        null));

        assertThat(response.intents()).contains(ChatIntent.BEST_SELLER, ChatIntent.CAMPAIGN_OFFERS);
        assertThat(response.products()).hasSize(1);
        assertThat(response.products().get(0).recommendedVariantId()).isEqualTo(101L);
        assertThat(response.products().get(0).recommendedVariantName()).isEqualTo("16GB / 512GB");
        assertThat(response.products().get(0).availableStock()).isEqualTo(8);
        assertThat(response.products().get(0).voucher().code()).isEqualTo("SCHOOL10");
        assertThat(response.products().get(0).reason()).contains("bán chạy").contains("voucher");
        ArgumentCaptor<ChatSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ChatSearchCriteria.class);
        verify(discoveryService).discover(
                criteriaCaptor.capture(), eq(true), eq(null), eq("session-1"));
        assertThat(criteriaCaptor.getValue().offerOnly()).isTrue();
        assertThat(criteriaCaptor.getValue().productScopeIds()).containsExactly(10L);
        verify(conversationStore).save(any(ChatConversationSnapshot.class));
        verify(analyticsEventService).track(eq(null), eq("session-1"), any());
    }

    @Test
    void answersGreetingWithoutCallingAiOrProductDiscovery() {
        when(conversationStore.find(any())).thenReturn(Optional.empty());

        var response = service.reply(
                null,
                "session-1",
                new ChatMessageRequest(null, "Xin chào bạn", null));

        assertThat(response.intents()).containsExactly(ChatIntent.GREETING);
        assertThat(response.answer()).contains("Xin chào").contains("trợ lý mua sắm");
        assertThat(response.products()).isEmpty();
        assertThat(response.quickReplies()).containsExactly(
                "Sản phẩm bán chạy",
                "Tư vấn theo nhu cầu",
                "Sản phẩm đang có voucher");
        verifyNoInteractions(aiGateway, campaignOfferService, discoveryService);
        verify(conversationStore).save(any(ChatConversationSnapshot.class));
    }

    @Test
    void deterministicLatestBudgetOverridesOlderAiBudget() {
        UUID conversationId = UUID.randomUUID();
        when(conversationStore.find(conversationId)).thenReturn(Optional.of(
                new ChatConversationSnapshot(
                        conversationId,
                        "s:session-1",
                        List.of(new ChatHistoryMessage("user", "Tìm laptop dưới 20 triệu")))));
        when(aiGateway.analyze(any(), any(), any())).thenReturn(Optional.of(
                new ChatIntentAnalysis(
                        List.of(ChatIntent.PRODUCT_DISCOVERY),
                        "laptop",
                        null,
                        null,
                        new BigDecimal("20000000"),
                        null,
                        Map.of(),
                        false,
                        null)));
        when(discoveryService.discover(any(), eq(false), eq(null), eq("session-1")))
                .thenReturn(List.of());

        service.reply(
                null,
                "session-1",
                new ChatMessageRequest(conversationId, "Tôi muốn laptop dưới 2 triệu", null));

        ArgumentCaptor<ChatSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ChatSearchCriteria.class);
        verify(discoveryService).discover(
                criteriaCaptor.capture(), eq(false), eq(null), eq("session-1"));
        assertThat(criteriaCaptor.getValue().query()).isEqualTo("laptop");
        assertThat(criteriaCaptor.getValue().maxPrice())
                .isEqualByComparingTo(new BigDecimal("2000000"));
    }

    @Test
    void voucherFollowUpUsesPreviousProductQueryWhenAiReturnsReferenceText() {
        UUID conversationId = UUID.randomUUID();
        when(conversationStore.find(conversationId)).thenReturn(Optional.of(
                new ChatConversationSnapshot(
                        conversationId,
                        "s:session-1",
                        List.of(new ChatHistoryMessage("user", "Gợi ý laptop dưới 20 triệu")))));
        when(aiGateway.analyze(any(), any(), any())).thenReturn(Optional.of(
                new ChatIntentAnalysis(
                        List.of(ChatIntent.CAMPAIGN_OFFERS),
                        "các sản phẩm trên đang áp dụng voucher",
                        null,
                        null,
                        null,
                        null,
                        Map.of(),
                        false,
                        null)));
        when(campaignOfferService.findEffectiveOffers(null)).thenReturn(List.of(offer()));
        when(discoveryService.discover(any(), eq(false), eq(null), eq("session-1")))
                .thenReturn(List.of(candidate()));

        service.reply(
                null,
                "session-1",
                new ChatMessageRequest(
                        conversationId,
                        "Trong các sản phẩm trên, sản phẩm nào đang có voucher?",
                        null));

        ArgumentCaptor<ChatSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ChatSearchCriteria.class);
        verify(discoveryService).discover(
                criteriaCaptor.capture(), eq(false), eq(null), eq("session-1"));
        assertThat(criteriaCaptor.getValue().query()).isEqualTo("laptop");
        assertThat(criteriaCaptor.getValue().maxPrice())
                .isEqualByComparingTo(new BigDecimal("20000000"));
        assertThat(criteriaCaptor.getValue().offerOnly()).isTrue();
    }

    @Test
    void treatsNeedAttributesAsSoftSignalsInsteadOfMandatorySearchTerms() {
        when(conversationStore.find(any())).thenReturn(Optional.empty());
        when(aiGateway.analyze(any(), any(), any())).thenReturn(Optional.of(
                new ChatIntentAnalysis(
                        List.of(ChatIntent.PRODUCT_DISCOVERY),
                        "laptop",
                        "laptop",
                        null,
                        new BigDecimal("20000000"),
                        null,
                        Map.of("purpose", "học tập", "battery", "pin tốt"),
                        false,
                        null)));
        when(discoveryService.discover(any(), eq(false), eq(null), eq("session-1")))
                .thenReturn(List.of(candidate()));

        service.reply(
                null,
                "session-1",
                new ChatMessageRequest(
                        null,
                        "Tôi cần laptop để học tập, pin tốt, dưới 20 triệu",
                        null));

        ArgumentCaptor<ChatSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ChatSearchCriteria.class);
        verify(discoveryService).discover(
                criteriaCaptor.capture(), eq(false), eq(null), eq("session-1"));
        assertThat(criteriaCaptor.getValue().query()).isEqualTo("laptop");
        assertThat(criteriaCaptor.getValue().maxPrice())
                .isEqualByComparingTo(new BigDecimal("20000000"));
    }

    @Test
    void doesNotSuggestProductsWhenNoCampaignOfferIsEffective() {
        when(conversationStore.find(any())).thenReturn(Optional.empty());
        when(aiGateway.analyze(any(), any(), any())).thenReturn(Optional.empty());
        when(campaignOfferService.findEffectiveOffers(null)).thenReturn(List.of());

        var response = service.reply(
                null,
                "session-1",
                new ChatMessageRequest(null, "Sản phẩm có voucher", null));

        assertThat(response.products()).isEmpty();
        assertThat(response.answer()).contains("chưa có campaign");
        verify(discoveryService, never()).discover(any(), any(Boolean.class), any(), any());
    }

    @Test
    void requiresSessionForAnonymousVisitor() {
        assertThatThrownBy(() -> service.reply(
                null, null, new ChatMessageRequest(null, "Laptop bán chạy", null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("X-Session-Id");
    }

    @Test
    void completesCodCheckoutConversationAndReturnsOrderSummary() {
        AtomicReference<ChatConversationSnapshot> stored = new AtomicReference<>();
        when(conversationStore.find(any())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get()));
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return null;
        }).when(conversationStore).save(any(ChatConversationSnapshot.class));
        when(orderService.createOrder(eq(7L), any(CreateOrderRequest.class)))
                .thenReturn(orderResponse());

        var methods = service.reply(
                7L, "session-1",
                new ChatMessageRequest(null, "Tôi muốn thanh toán", null, "SCHOOL10"));
        var cod = service.reply(
                7L, "session-1",
                new ChatMessageRequest(
                        methods.conversationId(), "Cash on Delivery (COD)", null, "SCHOOL10"));
        var address = service.reply(
                7L, "session-1",
                new ChatMessageRequest(
                        methods.conversationId(),
                        "123 Nguyễn Trãi, Phường 2, Quận 5, TP.HCM",
                        null,
                        "SCHOOL10"));
        var completed = service.reply(
                7L, "session-1",
                new ChatMessageRequest(
                        methods.conversationId(), "Bỏ qua ghi chú", null, "SCHOOL10"));

        assertThat(methods.intents()).containsExactly(ChatIntent.CHECKOUT);
        assertThat(methods.quickReplies()).contains(
                "Cash on Delivery (COD)", "PayGate E-Wallet / Card Gateway");
        assertThat(cod.answer()).contains("địa chỉ giao hàng");
        assertThat(address.answer()).contains("ghi chú giao hàng");
        assertThat(completed.answer()).contains("Đặt hàng COD thành công").contains("#88");
        assertThat(completed.order()).isNotNull();
        assertThat(completed.order().paymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(completed.order().paymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(stored.get().checkoutState()).isNull();
        assertThat(stored.get().messages())
                .noneMatch(message -> message.content().contains("123 Nguyễn Trãi"));

        ArgumentCaptor<CreateOrderRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(orderService).createOrder(eq(7L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().shippingAddress())
                .isEqualTo("123 Nguyễn Trãi, Phường 2, Quận 5, TP.HCM");
        assertThat(requestCaptor.getValue().note()).isNull();
        assertThat(requestCaptor.getValue().couponCode()).isEqualTo("SCHOOL10");
        assertThat(requestCaptor.getValue().paymentMethod()).isEqualTo(PaymentMethod.COD);
    }

    @Test
    void createsCreditCardOrderAndReturnsPaygatePaymentLink() {
        AtomicReference<ChatConversationSnapshot> stored = new AtomicReference<>();
        when(conversationStore.find(any())).thenAnswer(invocation ->
                Optional.ofNullable(stored.get()));
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return null;
        }).when(conversationStore).save(any(ChatConversationSnapshot.class));
        when(orderService.createOrder(eq(7L), any(CreateOrderRequest.class)))
                .thenReturn(creditCardOrderResponse());

        var card = service.reply(
                7L, "session-1",
                new ChatMessageRequest(
                        null, "PayGate E-Wallet / Card Gateway", null, "SCHOOL10"));
        var address = service.reply(
                7L, "session-1",
                new ChatMessageRequest(
                        card.conversationId(),
                        "123 Nguyễn Trãi, Phường 2, Quận 5, TP.HCM",
                        null,
                        "SCHOOL10"));
        var completed = service.reply(
                7L, "session-1",
                new ChatMessageRequest(
                        card.conversationId(), "Bỏ qua ghi chú", null, "SCHOOL10"));

        assertThat(card.answer()).contains("PayGate E-Wallet / Card Gateway");
        assertThat(address.answer()).contains("ghi chú giao hàng");
        assertThat(completed.answer()).contains("Thanh toán qua PayGate");
        assertThat(completed.order()).isNotNull();
        assertThat(completed.order().paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(completed.order().paymentStatus()).isEqualTo(PaymentStatus.PENDING_PAYGATE);
        assertThat(completed.order().paymentUrl()).isEqualTo("http://localhost:4201/checkout?token=CHK_CARD");

        ArgumentCaptor<CreateOrderRequest> requestCaptor =
                ArgumentCaptor.forClass(CreateOrderRequest.class);
        verify(orderService).createOrder(eq(7L), requestCaptor.capture());
        assertThat(requestCaptor.getValue().paymentMethod()).isEqualTo(PaymentMethod.CREDIT_CARD);
        assertThat(requestCaptor.getValue().couponCode()).isEqualTo("SCHOOL10");
    }

    @Test
    void requiresAuthenticationBeforeCollectingCodAddress() {
        when(conversationStore.find(any())).thenReturn(Optional.empty());

        var response = service.reply(
                null,
                "session-1",
                new ChatMessageRequest(null, "Tôi muốn thanh toán bằng COD", null));

        assertThat(response.answer()).contains("cần đăng nhập");
        verifyNoInteractions(orderService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Tôi muốn thanh toán",
            "Mình muốn checkout",
            "Hãy chốt đơn giúp tôi",
            "Tôi muốn trả tiền",
            "Tôi muốn đặt hàng"
    })
    void recognizesEquivalentCheckoutRequests(String message) {
        when(conversationStore.find(any())).thenReturn(Optional.empty());

        var response = service.reply(
                7L,
                "session-1",
                new ChatMessageRequest(null, message, null));

        assertThat(response.intents()).containsExactly(ChatIntent.CHECKOUT);
        assertThat(response.quickReplies()).contains("Cash on Delivery (COD)");
        verifyNoInteractions(aiGateway, discoveryService, orderService);
    }

    @Test
    void rejectsConversationOwnedByAnotherVisitor() {
        UUID conversationId = UUID.randomUUID();
        when(conversationStore.find(conversationId)).thenReturn(Optional.of(
                new ChatConversationSnapshot(conversationId, "s:other", List.of())));

        assertThatThrownBy(() -> service.reply(
                null,
                "session-1",
                new ChatMessageRequest(conversationId, "Laptop bán chạy", null)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("does not belong");
    }

    private ChatCampaignOffer offer() {
        return new ChatCampaignOffer(
                1L,
                "Back to School",
                "Campaign",
                5L,
                "SCHOOL10",
                DiscountType.PERCENT,
                BigDecimal.TEN,
                new BigDecimal("1000000"),
                BigDecimal.ZERO,
                PromotionScopeType.PRODUCT,
                LocalDateTime.now().plusDays(10),
                Map.of(ScopeRefType.PRODUCT, Set.of(10L)));
    }

    private ChatProductCandidate candidate() {
        return new ChatProductCandidate(
                10L,
                "laptop-pro",
                "Laptop Pro",
                "Description",
                3L,
                "Laptop",
                "MarketBrand",
                null,
                101L,
                "16GB / 512GB",
                new BigDecimal("18000000"),
                new BigDecimal("18000000"),
                new BigDecimal("19500000"),
                8);
    }

    private OrderResponse orderResponse() {
        return new OrderResponse(
                88L,
                7L,
                "customer",
                "customer@example.com",
                "123 Nguyễn Trãi, Phường 2, Quận 5, TP.HCM",
                new BigDecimal("16300000"),
                new BigDecimal("1800000"),
                new BigDecimal("100000"),
                "SCHOOL10",
                OrderStatus.CONFIRMED,
                null,
                List.of(),
                LocalDateTime.now(),
                LocalDateTime.now());
    }

    private OrderResponse creditCardOrderResponse() {
        LocalDateTime now = LocalDateTime.now();
        PaygatePayloadResponse payload = new PaygatePayloadResponse(
                89L,
                7L,
                "MARKETPLACE_MP",
                new BigDecimal("16300000"),
                new BigDecimal("16300000"),
                BigDecimal.ZERO,
                PaymentMethod.CREDIT_CARD.name(),
                "http://localhost:4201/checkout?token=CHK_CARD");
        return new OrderResponse(
                89L,
                7L,
                "customer",
                "customer@example.com",
                "123 Nguyễn Trãi, Phường 2, Quận 5, TP.HCM",
                new BigDecimal("16300000"),
                new BigDecimal("1800000"),
                new BigDecimal("100000"),
                "SCHOOL10",
                OrderStatus.PENDING,
                PaymentMethod.CREDIT_CARD,
                PaymentStatus.PENDING_PAYGATE,
                new BigDecimal("16300000"),
                BigDecimal.ZERO,
                null,
                "CHK_CARD",
                "http://localhost:4201/checkout?token=CHK_CARD",
                payload,
                now.plusMinutes(15),
                null,
                null,
                null,
                null,
                null,
                List.of(),
                now,
                now);
    }
}
