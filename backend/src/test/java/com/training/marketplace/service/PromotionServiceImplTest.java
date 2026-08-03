package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreatePromotionCodeRequest;
import com.training.marketplace.dto.request.ScopeRefDto;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.mapper.PromotionMapper;
import com.training.marketplace.repository.PromotionCodeRepository;
import com.training.marketplace.repository.PromotionCodeScopeRepository;
import com.training.marketplace.repository.PromotionRedemptionRepository;
import com.training.marketplace.service.impl.PromotionServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for coupon CRUD guards and order-time consume/refund (supports T-301..T-305 + DoD). */
@ExtendWith(MockitoExtension.class)
class PromotionServiceImplTest {

    @Mock private PromotionCodeRepository promotionCodeRepository;
    @Mock private PromotionCodeScopeRepository scopeRepository;
    @Mock private PromotionRedemptionRepository redemptionRepository;
    @Mock private PromotionMapper promotionMapper;
    @Mock private PromotionValidationService validationService;
    @Mock private CartService cartService;

    private PromotionServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        service = new PromotionServiceImpl(promotionCodeRepository, scopeRepository, redemptionRepository,
                promotionMapper, validationService, cartService);
    }

    private CreatePromotionCodeRequest createReq(String code, DiscountType type, String value,
                                                 PromotionScopeType scope, List<ScopeRefDto> scopes) {
        return new CreatePromotionCodeRequest(code, type, new BigDecimal(value), null, null,
                scope, scopes, null, null, null, null, true);
    }

    // ---------- create

    @Test
    @DisplayName("create: duplicate code -> DuplicateResourceException (409)")
    void create_duplicate() {
        when(promotionCodeRepository.existsByCode("SUMMER10")).thenReturn(true);
        assertThatThrownBy(() -> service.create(createReq("summer10", DiscountType.PERCENT, "10", PromotionScopeType.CART, null)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("create: code is normalized to upper-case before persisting")
    void create_normalizesCode() {
        when(promotionCodeRepository.existsByCode("SUMMER10")).thenReturn(false);
        when(promotionCodeRepository.save(any(PromotionCode.class))).thenAnswer(inv -> {
            PromotionCode p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        service.create(createReq("summer10", DiscountType.PERCENT, "10", PromotionScopeType.CART, null));

        ArgumentCaptor<PromotionCode> captor = ArgumentCaptor.forClass(PromotionCode.class);
        verify(promotionCodeRepository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("SUMMER10");
        assertThat(captor.getValue().getUsedCount()).isZero();
        assertThat(captor.getValue().getScopeType()).isEqualTo(PromotionScopeType.CART);
    }

    @Test
    @DisplayName("create: PERCENT value over 100 -> BadRequestException")
    void create_percentOver100() {
        assertThatThrownBy(() -> service.create(createReq("X", DiscountType.PERCENT, "150", PromotionScopeType.CART, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("create: PRODUCT scope without scope entries -> BadRequestException")
    void create_productScopeRequiresScopes() {
        assertThatThrownBy(() -> service.create(createReq("X", DiscountType.FIXED, "50", PromotionScopeType.PRODUCT, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("delete: soft-deletes by setting active=false")
    void delete_soft() {
        PromotionCode promo = PromotionCode.builder().code("SUMMER10").active(true).build();
        promo.setId(7L);
        when(promotionCodeRepository.findById(7L)).thenReturn(Optional.of(promo));

        service.delete(7L);

        assertThat(promo.isActive()).isFalse();
        verify(promotionCodeRepository).save(promo);
    }

    // ---------- consume (order-time)

    @Test
    @DisplayName("consume: valid coupon increments used_count and returns the discount")
    void consume_valid() {
        PromotionCode promo = PromotionCode.builder()
                .code("SUMMER10").discountType(DiscountType.PERCENT).discountValue(new BigDecimal("10"))
                .usageLimit(10).usedCount(3).build();
        promo.setId(7L);
        when(promotionCodeRepository.findByCodeForUpdate("SUMMER10")).thenReturn(Optional.of(promo));
        when(validationService.evaluate(eq("SUMMER10"), eq(1L), any())).thenReturn(
                PromotionEvaluation.ok(promo, new BigDecimal("500.00"), new BigDecimal("50.00"), new BigDecimal("500.00")));

        AppliedCoupon applied = service.consume("summer10", 1L, anyCart());

        assertThat(promo.getUsedCount()).isEqualTo(4);
        assertThat(applied.promotionCodeId()).isEqualTo(7L);
        assertThat(applied.code()).isEqualTo("SUMMER10");
        assertThat(applied.discountAmount()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("consume: invalid coupon (validation fails) -> BadRequestException, no increment")
    void consume_invalid() {
        PromotionCode promo = PromotionCode.builder().code("SUMMER10").usedCount(3).build();
        when(promotionCodeRepository.findByCodeForUpdate("SUMMER10")).thenReturn(Optional.of(promo));
        when(validationService.evaluate(eq("SUMMER10"), eq(1L), any())).thenReturn(
                PromotionEvaluation.invalid(com.training.marketplace.enums.PromotionReason.EXPIRED, promo, new BigDecimal("500.00")));

        assertThatThrownBy(() -> service.consume("SUMMER10", 1L, anyCart()))
                .isInstanceOf(BadRequestException.class);
        assertThat(promo.getUsedCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("consume: unknown code -> BadRequestException")
    void consume_codeNotFound() {
        when(promotionCodeRepository.findByCodeForUpdate("NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.consume("nope", 1L, anyCart()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("consume: usage limit hit under lock (racy valid eval) -> BadRequestException, no increment")
    void consume_usageLimitUnderLock() {
        PromotionCode promo = PromotionCode.builder()
                .code("SUMMER10").discountType(DiscountType.PERCENT).discountValue(new BigDecimal("10"))
                .usageLimit(10).usedCount(10).build();
        promo.setId(7L);
        when(promotionCodeRepository.findByCodeForUpdate("SUMMER10")).thenReturn(Optional.of(promo));
        when(validationService.evaluate(eq("SUMMER10"), eq(1L), any())).thenReturn(
                PromotionEvaluation.ok(promo, new BigDecimal("500.00"), new BigDecimal("50.00"), new BigDecimal("500.00")));

        assertThatThrownBy(() -> service.consume("SUMMER10", 1L, anyCart()))
                .isInstanceOf(BadRequestException.class);
        assertThat(promo.getUsedCount()).isEqualTo(10); // not incremented past the limit
    }

    // ---------- refund (cancel-time)

    @Test
    @DisplayName("refundIfPresent: decrements used_count and deletes the redemption row")
    void refund_present() {
        PromotionCode promo = PromotionCode.builder().code("SUMMER10").usedCount(5).build();
        promo.setId(7L);
        when(redemptionRepository.existsByOrderId(100L)).thenReturn(true);
        when(promotionCodeRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(promo));

        service.refundIfPresent(7L, 100L);

        assertThat(promo.getUsedCount()).isEqualTo(4);
        verify(redemptionRepository).deleteByOrderId(100L);
    }

    @Test
    @DisplayName("refundIfPresent: null coupon id is a no-op")
    void refund_nullNoop() {
        service.refundIfPresent(null, 100L);
        verify(redemptionRepository, never()).deleteByOrderId(any());
        verify(promotionCodeRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("refundIfPresent: no redemption for the order is a no-op (idempotent)")
    void refund_absentNoop() {
        when(redemptionRepository.existsByOrderId(100L)).thenReturn(false);
        service.refundIfPresent(7L, 100L);
        verify(promotionCodeRepository, never()).findByIdForUpdate(any());
        verify(redemptionRepository, never()).deleteByOrderId(any());
    }

    private CartResponse anyCart() {
        return new CartResponse(1L, List.of(), new BigDecimal("500.00"), BigDecimal.ZERO, 0);
    }
}
