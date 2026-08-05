package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateReviewRequest;
import com.training.marketplace.dto.request.UpdateReviewRequest;
import com.training.marketplace.dto.response.ProductRatingSummaryResponse;
import com.training.marketplace.dto.response.ProductReviewResponse;
import com.training.marketplace.dto.response.RatingSummaryResponse;
import com.training.marketplace.dto.response.ReviewEligibilityResponse;
import com.training.marketplace.enums.ReviewStatus;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    ReviewEligibilityResponse checkEligibility(Long productId, String username);

    ProductReviewResponse createReview(Long productId, String username, CreateReviewRequest request);

    ProductReviewResponse updateReview(Long reviewId, String username, UpdateReviewRequest request);

    void softDeleteReview(Long reviewId, String username);

    PageResponse<ProductReviewResponse> getProductReviews(Long productId, Integer ratingFilter, Pageable pageable);

    PageResponse<ProductReviewResponse> getReviewsForAdmin(Integer rating, ReviewStatus status, Long productId, Pageable pageable);

    RatingSummaryResponse getRatingSummary(Long productId);

    java.util.List<ProductRatingSummaryResponse> getRatingSummaries(java.util.List<Long> productIds);

    ProductReviewResponse adminUpdateStatus(Long reviewId, ReviewStatus status);

    ProductReviewResponse adminReply(Long reviewId, String reply);

    java.util.List<Long> getMyReviewedProductIds(String username);

    java.util.List<Long> getMyReviewedOrderItemIds(String username);
}
