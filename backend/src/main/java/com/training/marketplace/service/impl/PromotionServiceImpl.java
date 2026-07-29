package com.training.marketplace.service.impl;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreatePromotionCodeRequest;
import com.training.marketplace.dto.request.ScopeRefDto;
import com.training.marketplace.dto.request.UpdatePromotionCodeRequest;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.dto.response.CouponPreviewResponse;
import com.training.marketplace.dto.response.PromotionCodeResponse;
import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.entity.PromotionCodeScope;
import com.training.marketplace.entity.PromotionRedemption;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.PromotionMapper;
import com.training.marketplace.repository.PromotionCodeRepository;
import com.training.marketplace.repository.PromotionCodeScopeRepository;
import com.training.marketplace.repository.PromotionRedemptionRepository;
import com.training.marketplace.service.AppliedCoupon;
import com.training.marketplace.service.CartService;
import com.training.marketplace.service.PromotionEvaluation;
import com.training.marketplace.service.PromotionService;
import com.training.marketplace.service.PromotionValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionServiceImpl implements PromotionService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final PromotionCodeRepository promotionCodeRepository;
    private final PromotionCodeScopeRepository scopeRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final PromotionMapper promotionMapper;
    private final PromotionValidationService validationService;
    private final CartService cartService;

    // ---------------------------------------------------------------- CRUD

    @Override
    @Transactional
    public PromotionCodeResponse create(CreatePromotionCodeRequest request) {
        PromotionScopeType scopeType = request.scopeType() == null
                ? PromotionScopeType.CART : request.scopeType();
        validateDiscount(request.discountType(), request.discountValue());
        if (scopeType != PromotionScopeType.CART) {
            validateScopeRefs(scopeType, request.scopes());
        }

        String code = PromotionValidationService.normalize(request.code());
        if (promotionCodeRepository.existsByCode(code)) {
            throw new DuplicateResourceException("PromotionCode", "code", code);
        }

        PromotionCode promo = PromotionCode.builder()
                .code(code)
                .discountType(request.discountType())
                .discountValue(request.discountValue())
                .maxDiscount(request.maxDiscount())
                .minOrderAmount(request.minOrderAmount() == null ? BigDecimal.ZERO : request.minOrderAmount())
                .scopeType(scopeType)
                .usageLimit(request.usageLimit())
                .perUserLimit(request.perUserLimit())
                .usedCount(0)
                .startsAt(request.startsAt())
                .expiresAt(request.expiresAt())
                .active(request.active() == null || request.active())
                .build();
        PromotionCode saved = promotionCodeRepository.save(promo);

        if (scopeType != PromotionScopeType.CART) {
            insertScopes(saved.getId(), request.scopes());
        }
        log.info("Coupon created: id={}, code={}, type={}, scope={}", saved.getId(), code,
                saved.getDiscountType(), scopeType);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public PromotionCodeResponse update(Long id, UpdatePromotionCodeRequest request) {
        PromotionCode promo = promotionCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromotionCode", id));

        if (request.discountType() != null) promo.setDiscountType(request.discountType());
        if (request.discountValue() != null) promo.setDiscountValue(request.discountValue());
        if (request.maxDiscount() != null) promo.setMaxDiscount(request.maxDiscount());
        if (request.minOrderAmount() != null) promo.setMinOrderAmount(request.minOrderAmount());
        if (request.usageLimit() != null) promo.setUsageLimit(request.usageLimit());
        if (request.perUserLimit() != null) promo.setPerUserLimit(request.perUserLimit());
        if (request.startsAt() != null) promo.setStartsAt(request.startsAt());
        if (request.expiresAt() != null) promo.setExpiresAt(request.expiresAt());
        if (request.active() != null) promo.setActive(request.active());

        validateDiscount(promo.getDiscountType(), promo.getDiscountValue());
        applyScopeUpdate(promo, request);

        PromotionCode saved = promotionCodeRepository.save(promo);
        log.info("Coupon updated: id={}, code={}", saved.getId(), saved.getCode());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        PromotionCode promo = promotionCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromotionCode", id));
        promo.setActive(false); // soft delete
        promotionCodeRepository.save(promo);
        log.info("Coupon soft-deleted: id={}, code={}", id, promo.getCode());
    }

    @Override
    @Transactional(readOnly = true)
    public PromotionCodeResponse getById(Long id) {
        PromotionCode promo = promotionCodeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PromotionCode", id));
        return toResponse(promo);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<PromotionCodeResponse> list(Boolean active, DiscountType type, String q, Pageable pageable) {
        Page<PromotionCode> page = promotionCodeRepository.search(active, type, blankToNull(q), pageable);
        return PageResponse.from(page, this::toResponse);
    }

    // ---------------------------------------------------------------- Storefront

    @Override
    @Transactional(readOnly = true)
    public CouponPreviewResponse preview(Long userId, String code) {
        CartResponse cart = cartService.getCart(userId);
        PromotionEvaluation ev = validationService.evaluate(code, userId, cart);
        return new CouponPreviewResponse(
                PromotionValidationService.normalize(code),
                ev.valid(),
                ev.valid() ? null : ev.reason().name(),
                ev.promo() != null ? ev.promo().getDiscountType() : null,
                ev.eligibleSubtotal(),
                ev.discountAmount(),
                ev.cartSubtotal(),
                ev.newTotal());
    }

    // ---------------------------------------------------------------- Order-time

    @Override
    @Transactional
    public AppliedCoupon consume(String rawCode, Long userId, CartResponse cart) {
        String code = PromotionValidationService.normalize(rawCode);
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Coupon code is required");
        }
        // Lock the coupon row first so the used_count check-and-increment is atomic.
        PromotionCode promo = promotionCodeRepository.findByCodeForUpdate(code)
                .orElseThrow(() -> new BadRequestException("Coupon invalid: CODE_NOT_FOUND"));

        PromotionEvaluation ev = validationService.evaluate(code, userId, cart);
        if (!ev.valid()) {
            throw new BadRequestException("Coupon invalid: " + ev.reason());
        }
        // Authoritative re-check under the lock (guards the very last redemption slot).
        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            throw new BadRequestException("Coupon invalid: USAGE_LIMIT_REACHED");
        }

        promo.setUsedCount(promo.getUsedCount() + 1);
        return new AppliedCoupon(promo.getId(), promo.getCode(), ev.discountAmount());
    }

    @Override
    @Transactional
    public void recordRedemption(Long promotionCodeId, Long userId, Long orderId, BigDecimal discountAmount) {
        redemptionRepository.save(PromotionRedemption.builder()
                .promotionCodeId(promotionCodeId)
                .userId(userId)
                .orderId(orderId)
                .discountAmount(discountAmount)
                .build());
    }

    @Override
    @Transactional
    public void refundIfPresent(Long promotionCodeId, Long orderId) {
        if (promotionCodeId == null || !redemptionRepository.existsByOrderId(orderId)) {
            return; // nothing to refund — idempotent
        }
        promotionCodeRepository.findByIdForUpdate(promotionCodeId).ifPresent(promo -> {
            if (promo.getUsedCount() > 0) {
                promo.setUsedCount(promo.getUsedCount() - 1);
            }
        });
        redemptionRepository.deleteByOrderId(orderId);
        log.info("Coupon redemption refunded: promotionCodeId={}, orderId={}", promotionCodeId, orderId);
    }

    // ---------------------------------------------------------------- helpers

    private PromotionCodeResponse toResponse(PromotionCode promo) {
        List<PromotionCodeScope> scopes = scopeRepository.findByPromotionCodeId(promo.getId());
        return promotionMapper.toResponse(promo, scopes);
    }

    private void applyScopeUpdate(PromotionCode promo, UpdatePromotionCodeRequest request) {
        if (request.scopeType() != null) {
            promo.setScopeType(request.scopeType());
            if (request.scopeType() == PromotionScopeType.CART) {
                scopeRepository.deleteByPromotionCodeId(promo.getId());
            } else {
                validateScopeRefs(request.scopeType(), request.scopes());
                replaceScopes(promo.getId(), request.scopes());
            }
        } else if (request.scopes() != null) {
            if (promo.getScopeType() == PromotionScopeType.CART) {
                throw new BadRequestException("Cannot set scopes for a CART-scoped coupon");
            }
            validateScopeRefs(promo.getScopeType(), request.scopes());
            replaceScopes(promo.getId(), request.scopes());
        }
    }

    private void replaceScopes(Long promotionCodeId, List<ScopeRefDto> scopes) {
        scopeRepository.deleteByPromotionCodeId(promotionCodeId);
        insertScopes(promotionCodeId, scopes);
    }

    private void insertScopes(Long promotionCodeId, List<ScopeRefDto> scopes) {
        Set<ScopeRefDto> distinct = new LinkedHashSet<>(scopes);
        for (ScopeRefDto ref : distinct) {
            scopeRepository.save(PromotionCodeScope.builder()
                    .promotionCodeId(promotionCodeId)
                    .refType(ref.refType())
                    .refId(ref.refId())
                    .build());
        }
    }

    private void validateDiscount(DiscountType type, BigDecimal value) {
        if (type == DiscountType.PERCENT && value != null && value.compareTo(HUNDRED) > 0) {
            throw new BadRequestException("Percent discount cannot exceed 100");
        }
    }

    private void validateScopeRefs(PromotionScopeType scopeType, List<ScopeRefDto> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw new BadRequestException("scopes are required when scopeType is " + scopeType);
        }
        ScopeRefType expected = scopeType == PromotionScopeType.PRODUCT
                ? ScopeRefType.PRODUCT : ScopeRefType.CATEGORY;
        for (ScopeRefDto ref : scopes) {
            if (ref.refType() != expected) {
                throw new BadRequestException(
                        "scope refType must be " + expected + " when scopeType is " + scopeType);
            }
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
