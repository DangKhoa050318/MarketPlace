package com.training.marketplace.service;

import com.training.marketplace.dto.response.CartItemResponse;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.entity.PromotionCodeScope;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.enums.PromotionReason;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.PromotionCodeRepository;
import com.training.marketplace.repository.PromotionCodeScopeRepository;
import com.training.marketplace.repository.PromotionRedemptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates a coupon against the current cart and computes the discount (REQ-STP-B-305).
 * The cart is always supplied by the backend (fetched from Redis), never trusted from the client
 * (REQ-STP-T-305). Scope resolution walks variantId -> productId -> categoryId.
 */
@Service
@RequiredArgsConstructor
public class PromotionValidationService {

    private final PromotionCodeRepository promotionCodeRepository;
    private final PromotionCodeScopeRepository scopeRepository;
    private final PromotionRedemptionRepository redemptionRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final DiscountCalculationService discountCalculationService;

    /** Normalizes a coupon code to the stored form: trimmed, upper-case. Null-safe. */
    public static String normalize(String rawCode) {
        return rawCode == null ? null : rawCode.trim().toUpperCase();
    }

    @Transactional(readOnly = true)
    public PromotionEvaluation evaluate(String rawCode, Long userId, CartResponse cart) {
        BigDecimal cartSubtotal = nz(cart == null ? null : cart.totalAmount());

        String code = normalize(rawCode);
        if (code == null || code.isBlank()) {
            return PromotionEvaluation.invalid(PromotionReason.EMPTY_CODE, null, cartSubtotal);
        }

        PromotionCode promo = promotionCodeRepository.findByCode(code).orElse(null);
        if (promo == null) {
            return PromotionEvaluation.invalid(PromotionReason.CODE_NOT_FOUND, null, cartSubtotal);
        }
        if (!promo.isActive()) {
            return PromotionEvaluation.invalid(PromotionReason.INACTIVE, promo, cartSubtotal);
        }

        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartsAt() != null && now.isBefore(promo.getStartsAt())) {
            return PromotionEvaluation.invalid(PromotionReason.NOT_STARTED, promo, cartSubtotal);
        }
        if (promo.getExpiresAt() != null && now.isAfter(promo.getExpiresAt())) {
            return PromotionEvaluation.invalid(PromotionReason.EXPIRED, promo, cartSubtotal);
        }
        if (promo.getUsageLimit() != null && promo.getUsedCount() >= promo.getUsageLimit()) {
            return PromotionEvaluation.invalid(PromotionReason.USAGE_LIMIT_REACHED, promo, cartSubtotal);
        }
        if (promo.getPerUserLimit() != null && userId != null
                && redemptionRepository.countByPromotionCodeIdAndUserId(promo.getId(), userId) >= promo.getPerUserLimit()) {
            return PromotionEvaluation.invalid(PromotionReason.PER_USER_LIMIT_REACHED, promo, cartSubtotal);
        }

        BigDecimal eligibleSubtotal = eligibleSubtotal(promo, cart);
        if (eligibleSubtotal.signum() <= 0) {
            return PromotionEvaluation.invalid(PromotionReason.NO_ELIGIBLE_ITEMS, promo, cartSubtotal);
        }
        // Minimum-order threshold is checked against the whole cart subtotal.
        if (cartSubtotal.compareTo(nz(promo.getMinOrderAmount())) < 0) {
            return PromotionEvaluation.invalid(PromotionReason.MIN_ORDER_NOT_MET, promo, cartSubtotal);
        }

        BigDecimal discount = discountCalculationService.calculate(promo, eligibleSubtotal);
        return PromotionEvaluation.ok(promo, eligibleSubtotal, discount, cartSubtotal);
    }

    /** Sum of item subtotals that fall inside the coupon's scope. CART = whole cart. */
    private BigDecimal eligibleSubtotal(PromotionCode promo, CartResponse cart) {
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (promo.getScopeType() == PromotionScopeType.CART) {
            return nz(cart.totalAmount());
        }

        List<PromotionCodeScope> scopes = scopeRepository.findByPromotionCodeId(promo.getId());
        ScopeRefType wanted = promo.getScopeType() == PromotionScopeType.PRODUCT
                ? ScopeRefType.PRODUCT : ScopeRefType.CATEGORY;
        Set<Long> refIds = scopes.stream()
                .filter(s -> s.getRefType() == wanted)
                .map(PromotionCodeScope::getRefId)
                .collect(Collectors.toSet());
        if (refIds.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // Resolve variantId -> productId for every cart item.
        List<Long> variantIds = cart.items().stream()
                .map(CartItemResponse::variantId).distinct().toList();
        Map<Long, Long> variantToProduct = variantRepository.findAllById(variantIds).stream()
                .collect(Collectors.toMap(ProductVariant::getId, ProductVariant::getProductId));

        // For CATEGORY scope, also resolve productId -> categoryId.
        Map<Long, Long> productToCategory = Map.of();
        if (promo.getScopeType() == PromotionScopeType.CATEGORY) {
            List<Long> productIds = variantToProduct.values().stream().distinct().toList();
            productToCategory = productRepository.findAllById(productIds).stream()
                    .filter(p -> p.getCategoryId() != null)
                    .collect(Collectors.toMap(Product::getId, Product::getCategoryId));
        }

        BigDecimal sum = BigDecimal.ZERO;
        for (CartItemResponse item : cart.items()) {
            Long productId = variantToProduct.get(item.variantId());
            if (productId == null) {
                continue;
            }
            boolean matches;
            if (promo.getScopeType() == PromotionScopeType.PRODUCT) {
                matches = refIds.contains(productId);
            } else {
                Long categoryId = productToCategory.get(productId);
                matches = categoryId != null && refIds.contains(categoryId);
            }
            if (matches) {
                sum = sum.add(nz(item.subtotal()));
            }
        }
        return sum;
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
