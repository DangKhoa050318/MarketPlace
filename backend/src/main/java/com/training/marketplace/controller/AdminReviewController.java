package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AdminReviewReplyRequest;
import com.training.marketplace.dto.request.AdminUpdateReviewStatusRequest;
import com.training.marketplace.dto.response.ProductReviewResponse;
import com.training.marketplace.enums.ReviewStatus;
import com.training.marketplace.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@Tag(name = "Admin Review Moderation", description = "Admin & Manager endpoints for moderating customer reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @Operation(summary = "List reviews for moderation",
            description = "Paginated reviews filterable by star rating (1-5), status and product")
    public ApiResponse<PageResponse<ProductReviewResponse>> listReviews(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) Long productId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ProductReviewResponse> response = reviewService.getReviewsForAdmin(rating, status, productId, pageable);
        return ApiResponse.success(response);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Moderate review status", description = "Approve, hide, or soft delete a customer review")
    public ApiResponse<ProductReviewResponse> updateReviewStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateReviewStatusRequest request) {
        ProductReviewResponse response = reviewService.adminUpdateStatus(id, request.status());
        return ApiResponse.success("Review status updated successfully", response);
    }

    @PutMapping("/{id}/reply")
    @Operation(summary = "Reply to a review", description = "Shop/seller public reply shown under the customer review")
    public ApiResponse<ProductReviewResponse> replyToReview(
            @PathVariable Long id,
            @Valid @RequestBody AdminReviewReplyRequest request) {
        ProductReviewResponse response = reviewService.adminReply(id, request.reply());
        return ApiResponse.success("Reply saved", response);
    }
}

