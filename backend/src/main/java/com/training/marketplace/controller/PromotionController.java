package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.ApplyCouponRequest;
import com.training.marketplace.dto.request.CreatePromotionCodeRequest;
import com.training.marketplace.dto.request.UpdatePromotionCodeRequest;
import com.training.marketplace.dto.response.CouponPreviewResponse;
import com.training.marketplace.dto.response.PromotionCodeResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.AuditService;
import com.training.marketplace.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Coupon / promotion endpoints (FEATURE-STP-02). {@code /preview} is for shoppers (backend reads
 * their cart); the CRUD endpoints are ADMIN-only (enforced by {@code SecurityConfig}).
 */
@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Coupon preview (customer) and management (admin)")
public class PromotionController {

    private final PromotionService promotionService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @PostMapping("/preview")
    @Operation(summary = "Preview a coupon against the current user's cart")
    public ApiResponse<CouponPreviewResponse> preview(
            Authentication authentication,
            @Valid @RequestBody ApplyCouponRequest request) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(promotionService.preview(userId, request.code()));
    }

    @GetMapping
    @Operation(summary = "List coupons with filters (ADMIN)")
    public ApiResponse<PageResponse<PromotionCodeResponse>> list(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) DiscountType type,
            @RequestParam(required = false) String q,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(promotionService.list(active, type, q, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a coupon by id (ADMIN)")
    public ApiResponse<PromotionCodeResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(promotionService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a coupon (ADMIN)")
    public ApiResponse<PromotionCodeResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreatePromotionCodeRequest request) {
        PromotionCodeResponse created = promotionService.create(request);
        auditService.record(actor(authentication), "COUPON_CREATE", "PromotionCode", created.id(),
                "code=" + created.code() + ", type=" + created.discountType() + ", scope=" + created.scopeType());
        return ApiResponse.success("Coupon created", created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a coupon — code is immutable (ADMIN)")
    public ApiResponse<PromotionCodeResponse> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromotionCodeRequest request) {
        PromotionCodeResponse updated = promotionService.update(id, request);
        auditService.record(actor(authentication), "COUPON_UPDATE", "PromotionCode", id,
                "code=" + updated.code() + ", active=" + updated.active());
        return ApiResponse.success("Coupon updated", updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft-delete a coupon (ADMIN)")
    public void delete(Authentication authentication, @PathVariable Long id) {
        promotionService.delete(id);
        auditService.record(actor(authentication), "COUPON_DELETE", "PromotionCode", id, "soft delete (active=false)");
    }

    private String actor(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }

    private Long getUserId(Authentication authentication) {
        Authentication auth = authentication != null
                ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Unauthenticated user context");
        }
        String identifier = auth.getName();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier));
        return user.getId();
    }
}
