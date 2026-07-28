package com.training.marketplace.service;

import com.training.marketplace.dto.response.CartItemResponse;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.entity.PromotionCodeScope;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionReason;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.PromotionCodeRepository;
import com.training.marketplace.repository.PromotionCodeScopeRepository;
import com.training.marketplace.repository.PromotionRedemptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** REQ-STP-T-302 (time/inactive/case) and REQ-STP-T-303 (scope) for coupon validation. */
@ExtendWith(MockitoExtension.class)
class PromotionValidationServiceTest {

    @Mock private PromotionCodeRepository promotionCodeRepository;
    @Mock private PromotionCodeScopeRepository scopeRepository;
    @Mock private PromotionRedemptionRepository redemptionRepository;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;

    private PromotionValidationService service;

    @BeforeEach
    void setUp() {
        service = new PromotionValidationService(promotionCodeRepository, scopeRepository,
                redemptionRepository, variantRepository, productRepository, new DiscountCalculationService());
    }

    // ------- builders

    private PromotionCode.PromotionCodeBuilder validCart() {
        return PromotionCode.builder()
                .code("SUMMER10").discountType(DiscountType.PERCENT).discountValue(new BigDecimal("10"))
                .minOrderAmount(BigDecimal.ZERO).scopeType(PromotionScopeType.CART).usedCount(0).active(true);
    }

    private CartItemResponse item(long variantId, String subtotal) {
        return new CartItemResponse(variantId, "SKU" + variantId, "Product", "Variant",
                new BigDecimal(subtotal), 1, new BigDecimal(subtotal), null);
    }

    private CartResponse cart(List<CartItemResponse> items) {
        BigDecimal total = items.stream().map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartResponse(1L, items, total, items.size());
    }

    // ------- T-302

    @Test
    @DisplayName("Coupon not yet started is rejected")
    void notStarted() {
        PromotionCode promo = validCart().startsAt(LocalDateTime.now().plusDays(1)).build();
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));

        PromotionEvaluation ev = service.evaluate("SUMMER10", 1L, cart(List.of(item(10, "500.00"))));

        assertThat(ev.valid()).isFalse();
        assertThat(ev.reason()).isEqualTo(PromotionReason.NOT_STARTED);
    }

    @Test
    @DisplayName("Expired coupon is rejected")
    void expired() {
        PromotionCode promo = validCart().expiresAt(LocalDateTime.now().minusDays(1)).build();
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));

        PromotionEvaluation ev = service.evaluate("SUMMER10", 1L, cart(List.of(item(10, "500.00"))));

        assertThat(ev.reason()).isEqualTo(PromotionReason.EXPIRED);
    }

    @Test
    @DisplayName("Inactive coupon is rejected")
    void inactive() {
        PromotionCode promo = validCart().active(false).build();
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));

        PromotionEvaluation ev = service.evaluate("SUMMER10", 1L, cart(List.of(item(10, "500.00"))));

        assertThat(ev.reason()).isEqualTo(PromotionReason.INACTIVE);
    }

    @Test
    @DisplayName("Codes are matched case-insensitively (normalized to upper-case)")
    void caseInsensitive() {
        PromotionCode promo = validCart().build();
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));

        PromotionEvaluation ev = service.evaluate("summer10", 1L, cart(List.of(item(10, "500.00"))));

        assertThat(ev.valid()).isTrue();
        assertThat(ev.discountAmount()).isEqualByComparingTo("50.00");
        assertThat(ev.newTotal()).isEqualByComparingTo("450.00");
        verify(promotionCodeRepository).findByCode("SUMMER10");
    }

    @Test
    @DisplayName("Unknown code -> CODE_NOT_FOUND")
    void codeNotFound() {
        when(promotionCodeRepository.findByCode("NOPE")).thenReturn(Optional.empty());
        assertThat(service.evaluate("nope", 1L, cart(List.of(item(10, "500.00")))).reason())
                .isEqualTo(PromotionReason.CODE_NOT_FOUND);
    }

    @Test
    @DisplayName("Blank code -> EMPTY_CODE")
    void emptyCode() {
        assertThat(service.evaluate("  ", 1L, cart(List.of(item(10, "500.00")))).reason())
                .isEqualTo(PromotionReason.EMPTY_CODE);
    }

    @Test
    @DisplayName("Below minimum order amount is rejected")
    void minOrderNotMet() {
        PromotionCode promo = validCart().minOrderAmount(new BigDecimal("1000")).build();
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));

        assertThat(service.evaluate("SUMMER10", 1L, cart(List.of(item(10, "200.00")))).reason())
                .isEqualTo(PromotionReason.MIN_ORDER_NOT_MET);
    }

    @Test
    @DisplayName("Total usage limit reached is rejected")
    void usageLimitReached() {
        PromotionCode promo = validCart().usageLimit(10).usedCount(10).build();
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));

        assertThat(service.evaluate("SUMMER10", 1L, cart(List.of(item(10, "500.00")))).reason())
                .isEqualTo(PromotionReason.USAGE_LIMIT_REACHED);
    }

    @Test
    @DisplayName("Per-user limit reached is rejected")
    void perUserLimitReached() {
        PromotionCode promo = validCart().perUserLimit(1).build();
        promo.setId(7L);
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));
        when(redemptionRepository.countByPromotionCodeIdAndUserId(7L, 1L)).thenReturn(1L);

        assertThat(service.evaluate("SUMMER10", 1L, cart(List.of(item(10, "500.00")))).reason())
                .isEqualTo(PromotionReason.PER_USER_LIMIT_REACHED);
    }

    // ------- T-303 scope

    @Test
    @DisplayName("CART scope: discount computed on whole cart subtotal")
    void scopeCart() {
        PromotionCode promo = validCart().build();
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));

        PromotionEvaluation ev = service.evaluate("SUMMER10", 1L,
                cart(List.of(item(10, "300.00"), item(20, "200.00"))));

        assertThat(ev.valid()).isTrue();
        assertThat(ev.eligibleSubtotal()).isEqualByComparingTo("500.00");
        assertThat(ev.discountAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("PRODUCT scope: only matching product's items count toward the discount")
    void scopeProductMixed() {
        PromotionCode promo = validCart().scopeType(PromotionScopeType.PRODUCT).build();
        promo.setId(7L);
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));
        when(scopeRepository.findByPromotionCodeId(7L)).thenReturn(List.of(scope(ScopeRefType.PRODUCT, 100L)));
        when(variantRepository.findAllById(any())).thenReturn(List.of(
                variant(10L, 100L), variant(20L, 200L)));

        PromotionEvaluation ev = service.evaluate("SUMMER10", 1L,
                cart(List.of(item(10, "300.00"), item(20, "200.00"))));

        assertThat(ev.valid()).isTrue();
        assertThat(ev.eligibleSubtotal()).isEqualByComparingTo("300.00"); // only variant 10 (product 100)
        assertThat(ev.discountAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("CATEGORY scope: only items whose product's category matches count")
    void scopeCategory() {
        PromotionCode promo = validCart().scopeType(PromotionScopeType.CATEGORY).build();
        promo.setId(7L);
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));
        when(scopeRepository.findByPromotionCodeId(7L)).thenReturn(List.of(scope(ScopeRefType.CATEGORY, 5L)));
        when(variantRepository.findAllById(any())).thenReturn(List.of(
                variant(10L, 100L), variant(20L, 200L)));
        when(productRepository.findAllById(any())).thenReturn(List.of(
                product(100L, 5L), product(200L, 9L)));

        PromotionEvaluation ev = service.evaluate("SUMMER10", 1L,
                cart(List.of(item(10, "300.00"), item(20, "200.00"))));

        assertThat(ev.eligibleSubtotal()).isEqualByComparingTo("300.00"); // only product 100 (category 5)
    }

    @Test
    @DisplayName("PRODUCT scope with no matching items -> NO_ELIGIBLE_ITEMS")
    void scopeNoEligibleItems() {
        PromotionCode promo = validCart().scopeType(PromotionScopeType.PRODUCT).build();
        promo.setId(7L);
        when(promotionCodeRepository.findByCode("SUMMER10")).thenReturn(Optional.of(promo));
        when(scopeRepository.findByPromotionCodeId(7L)).thenReturn(List.of(scope(ScopeRefType.PRODUCT, 999L)));
        lenient().when(variantRepository.findAllById(any())).thenReturn(List.of(variant(10L, 100L)));

        assertThat(service.evaluate("SUMMER10", 1L, cart(List.of(item(10, "300.00")))).reason())
                .isEqualTo(PromotionReason.NO_ELIGIBLE_ITEMS);
    }

    private PromotionCodeScope scope(ScopeRefType type, Long refId) {
        return PromotionCodeScope.builder().promotionCodeId(7L).refType(type).refId(refId).build();
    }

    private ProductVariant variant(long id, long productId) {
        ProductVariant v = ProductVariant.builder().productId(productId).build();
        v.setId(id);
        return v;
    }

    private Product product(long id, long categoryId) {
        Product p = Product.builder().categoryId(categoryId).build();
        p.setId(id);
        return p;
    }
}
