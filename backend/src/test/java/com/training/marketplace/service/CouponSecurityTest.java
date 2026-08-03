package com.training.marketplace.service;

import com.training.marketplace.dto.response.CartItemResponse;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.dto.response.CouponPreviewResponse;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.entity.PromotionCodeScope;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;
import com.training.marketplace.mapper.PromotionMapper;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.PromotionCodeRepository;
import com.training.marketplace.repository.PromotionCodeScopeRepository;
import com.training.marketplace.repository.PromotionRedemptionRepository;
import com.training.marketplace.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REQ-STP-T-305: the client cannot influence subtotal, discount or coupon scope. The API only ever
 * accepts a coupon {@code code} (ApplyCouponRequest) or {@code couponCode} (CreateOrderRequest);
 * the backend reads the cart itself and takes the scope from the coupon record. These tests prove
 * the computed numbers track the SERVER cart and the coupon, never any client-supplied value.
 */
@ExtendWith(MockitoExtension.class)
class CouponSecurityTest {

    @Mock private PromotionCodeRepository promotionCodeRepository;
    @Mock private PromotionCodeScopeRepository scopeRepository;
    @Mock private PromotionRedemptionRepository redemptionRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private PromotionMapper promotionMapper;
    @Mock private CartService cartService;

    private PromotionServiceImpl service;

    @BeforeEach
    void setUp() {
        PromotionValidationService validation = new PromotionValidationService(
                promotionCodeRepository, scopeRepository, redemptionRepository,
                variantRepository, productRepository, new DiscountCalculationService());
        service = new PromotionServiceImpl(promotionCodeRepository, scopeRepository, redemptionRepository,
                promotionMapper, validation, cartService);
    }

    private PromotionCode percentCoupon(int percent) {
        return PromotionCode.builder()
                .code("SAVE" + percent).discountType(DiscountType.PERCENT)
                .discountValue(new BigDecimal(percent)).minOrderAmount(BigDecimal.ZERO)
                .scopeType(PromotionScopeType.CART).usedCount(0).active(true).build();
    }

    private CartItemResponse item(long variantId, String subtotal) {
        return new CartItemResponse(variantId, 1L, "Product", "Variant", "SKU" + variantId,
                new BigDecimal(subtotal), 1, new BigDecimal(subtotal), null);
    }

    private CartResponse cart(List<CartItemResponse> items) {
        BigDecimal total = items.stream().map(CartItemResponse::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(1L, items, total, BigDecimal.ZERO, items.size());
    }

    @Test
    @DisplayName("preview computes the discount from the server-read cart, not from any client value")
    void previewUsesServerCart() {
        PromotionCode coupon = percentCoupon(10);
        when(promotionCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(cartService.getCart(1L)).thenReturn(cart(List.of(item(10, "500.00"))));

        CouponPreviewResponse res = service.preview(1L, "save10");

        // Numbers derive purely from the server cart (subtotal 500) and the coupon (10%).
        assertThat(res.cartSubtotal()).isEqualByComparingTo("500.00");
        assertThat(res.discountAmount()).isEqualByComparingTo("50.00");
        assertThat(res.newTotal()).isEqualByComparingTo("450.00");
        verify(cartService).getCart(1L); // backend fetched the cart itself
    }

    @Test
    @DisplayName("discount tracks the server cart total, so a client cannot pin a bigger discount")
    void discountFollowsServerCart() {
        PromotionCode coupon = percentCoupon(10);
        when(promotionCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));

        // Same coupon, two different server carts -> two different server-computed discounts.
        when(cartService.getCart(1L)).thenReturn(cart(List.of(item(10, "200.00"))));
        assertThat(service.preview(1L, "SAVE10").discountAmount()).isEqualByComparingTo("20.00");

        when(cartService.getCart(1L)).thenReturn(cart(List.of(item(10, "1000.00"))));
        assertThat(service.preview(1L, "SAVE10").discountAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("scope comes from the coupon record, not the client: only in-scope items are discounted")
    void scopeTakenFromCouponNotClient() {
        PromotionCode coupon = percentCoupon(10);
        coupon.setScopeType(PromotionScopeType.PRODUCT);
        coupon.setId(7L);
        when(promotionCodeRepository.findByCode("SAVE10")).thenReturn(Optional.of(coupon));
        when(scopeRepository.findByPromotionCodeId(7L))
                .thenReturn(List.of(PromotionCodeScope.builder()
                        .promotionCodeId(7L).refType(ScopeRefType.PRODUCT).refId(100L).build()));
        // cart: product 100 (in scope, 300) + product 200 (out of scope, 200)
        when(variantRepository.findAllById(any())).thenReturn(List.of(variant(10, 100), variant(20, 200)));
        when(cartService.getCart(1L)).thenReturn(cart(List.of(item(10, "300.00"), item(20, "200.00"))));

        CouponPreviewResponse res = service.preview(1L, "SAVE10");

        // eligible = 300 (only product 100); discount 10% of 300 = 30 — the client can't widen the scope.
        assertThat(res.eligibleSubtotal()).isEqualByComparingTo("300.00");
        assertThat(res.discountAmount()).isEqualByComparingTo("30.00");
        assertThat(res.cartSubtotal()).isEqualByComparingTo("500.00");
    }

    private ProductVariant variant(long id, long productId) {
        ProductVariant v = ProductVariant.builder().productId(productId).build();
        v.setId(id);
        return v;
    }
}
