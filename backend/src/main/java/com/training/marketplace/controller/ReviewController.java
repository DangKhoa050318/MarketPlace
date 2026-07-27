package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateReviewRequest;
import com.training.marketplace.dto.request.UpdateReviewRequest;
import com.training.marketplace.dto.response.ProductReviewResponse;
import com.training.marketplace.dto.response.RatingSummaryResponse;
import com.training.marketplace.dto.response.ReviewEligibilityResponse;
import com.training.marketplace.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Product Reviews", description = "Public and customer endpoints for product ratings and reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/v1/products/{productId}/reviews")
    @Operation(summary = "Get public product reviews", description = "Retrieve approved reviews for a product with optional star rating filter, pagination and sorting")
    public ResponseEntity<ApiResponse<PageResponse<ProductReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer rating,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<ProductReviewResponse> response = reviewService.getProductReviews(productId, rating, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/products/{productId}/reviews/summary")
    @Operation(summary = "Get product rating summary", description = "Retrieve aggregate average rating, total reviews and 1-5 star breakdown")
    public ResponseEntity<ApiResponse<RatingSummaryResponse>> getRatingSummary(@PathVariable Long productId) {
        RatingSummaryResponse summary = reviewService.getRatingSummary(productId);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/api/v1/products/{productId}/reviews/eligibility")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Check user review eligibility", description = "Verify whether current user can submit a review and if verified purchase badge applies")
    public ResponseEntity<ApiResponse<ReviewEligibilityResponse>> checkEligibility(
            @PathVariable Long productId,
            Authentication authentication) {
        ReviewEligibilityResponse response = reviewService.checkEligibility(productId, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/api/v1/products/{productId}/reviews")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Create product review", description = "Submit a new rating and review for a product")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody CreateReviewRequest request,
            Authentication authentication) {
        ProductReviewResponse response = reviewService.createReview(productId, authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Review created successfully", response));
    }

    @PutMapping("/api/v1/reviews/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Update user review", description = "Update rating, title, and content of an existing review owned by current user")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> updateReview(
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request,
            Authentication authentication) {
        ProductReviewResponse response = reviewService.updateReview(id, authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully", response));
    }

    @DeleteMapping("/api/v1/reviews/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Delete user review", description = "Soft delete a product review owned by current user")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long id,
            Authentication authentication) {
        reviewService.softDeleteReview(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
    }
}
