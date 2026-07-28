package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreatePromotionCodeRequest;
import com.training.marketplace.dto.request.UpdatePromotionCodeRequest;
import com.training.marketplace.dto.response.CouponPreviewResponse;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.dto.response.PromotionCodeResponse;
import com.training.marketplace.enums.DiscountType;
import org.springframework.data.domain.Pageable;

public interface PromotionService {

    // --- Admin CRUD ---
    PromotionCodeResponse create(CreatePromotionCodeRequest request);

    PromotionCodeResponse update(Long id, UpdatePromotionCodeRequest request);

    void delete(Long id); // soft delete (active=false)

    PromotionCodeResponse getById(Long id);

    PageResponse<PromotionCodeResponse> list(Boolean active, DiscountType type, String q, Pageable pageable);

    // --- Storefront ---
    /** Preview a coupon against the caller's current cart (backend reads the cart itself). */
    CouponPreviewResponse preview(Long userId, String code);

    // --- Order-time integration (called inside the order transaction) ---
    /**
     * Locks the coupon row, validates it against the cart, increments {@code usedCount} and returns
     * the discount to apply. Throws {@link com.training.marketplace.exception.BadRequestException}
     * if the coupon is invalid. Does not write the redemption row yet (needs the order id).
     */
    AppliedCoupon consume(String code, Long userId, CartResponse cart);

    /** Writes the redemption ledger row once the order id is known. */
    void recordRedemption(Long promotionCodeId, Long userId, Long orderId, java.math.BigDecimal discountAmount);

    /** Refunds a redemption when its order is cancelled: decrements usedCount and deletes the row. Idempotent. */
    void refundIfPresent(Long promotionCodeId, Long orderId);
}
