package com.training.marketplace.service.impl;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateReviewRequest;
import com.training.marketplace.dto.request.UpdateReviewRequest;
import com.training.marketplace.dto.response.ProductReviewResponse;
import com.training.marketplace.dto.response.RatingSummaryResponse;
import com.training.marketplace.dto.response.ReviewEligibilityResponse;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductReview;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.ReviewStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ForbiddenException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.OrderItemRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductReviewRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    @Override
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse checkEligibility(Long productId, String username) {
        Product product = getProductOrThrow(productId);
        User user = getUserOrThrow(username);

        Optional<ProductReview> existingReviewOpt = reviewRepository.findByUserIdAndProductIdAndDeletedAtIsNull(user.getId(), product.getId());
        ProductReviewResponse existingResponse = existingReviewOpt.map(ProductReviewResponse::from).orElse(null);

        List<OrderItem> eligibleItems = orderItemRepository.findEligibleOrderItemsForReview(user.getId(), product.getId());
        boolean isVerified = !eligibleItems.isEmpty();
        Long orderItemId = isVerified ? eligibleItems.get(0).getId() : null;

        if (existingReviewOpt.isPresent()) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .isVerifiedPurchase(isVerified)
                    .orderItemId(orderItemId)
                    .existingReview(existingResponse)
                    .message("You have already reviewed this product.")
                    .build();
        }

        return ReviewEligibilityResponse.builder()
                .eligible(true)
                .isVerifiedPurchase(isVerified)
                .orderItemId(orderItemId)
                .existingReview(null)
                .message(isVerified ? "Eligible to write a verified purchase review." : "Eligible to write a review.")
                .build();
    }

    @Override
    @Transactional
    public ProductReviewResponse createReview(Long productId, String username, CreateReviewRequest request) {
        Product product = getProductOrThrow(productId);
        User user = getUserOrThrow(username);

        if (reviewRepository.existsByUserIdAndProductIdAndDeletedAtIsNull(user.getId(), product.getId())) {
            throw new DuplicateResourceException("Review", "user_id/product_id", user.getId() + "/" + product.getId());
        }

        List<OrderItem> eligibleItems = orderItemRepository.findEligibleOrderItemsForReview(user.getId(), product.getId());
        boolean isVerified = !eligibleItems.isEmpty();
        OrderItem orderItem = isVerified ? eligibleItems.get(0) : null;

        String sanitizedTitle = request.title().trim();
        String sanitizedContent = request.content().trim();

        ProductReview review = ProductReview.builder()
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .rating(request.rating())
                .title(sanitizedTitle)
                .content(sanitizedContent)
                .status(ReviewStatus.APPROVED)
                .isVerifiedPurchase(isVerified)
                .isEdited(false)
                .build();

        ProductReview savedReview = reviewRepository.save(review);
        return ProductReviewResponse.from(savedReview);
    }

    @Override
    @Transactional
    public ProductReviewResponse updateReview(Long reviewId, String username, UpdateReviewRequest request) {
        ProductReview review = getReviewOrThrow(reviewId);
        User currentUser = getUserOrThrow(username);

        boolean isOwner = review.getUser().getId().equals(currentUser.getId());
        boolean isAdminOrManager = currentUser.getRole().name().equals("ADMIN") || currentUser.getRole().name().equals("MANAGER");

        if (!isOwner && !isAdminOrManager) {
            throw new ForbiddenException("You do not have permission to update this review");
        }

        if (review.getStatus() == ReviewStatus.DELETED || review.getDeletedAt() != null) {
            throw new BadRequestException("Cannot update a deleted review");
        }

        review.setRating(request.rating());
        review.setTitle(request.title().trim());
        review.setContent(request.content().trim());
        review.setIsEdited(true);

        ProductReview updated = reviewRepository.save(review);
        return ProductReviewResponse.from(updated);
    }

    @Override
    @Transactional
    public void softDeleteReview(Long reviewId, String username) {
        ProductReview review = getReviewOrThrow(reviewId);
        User currentUser = getUserOrThrow(username);

        boolean isOwner = review.getUser().getId().equals(currentUser.getId());
        boolean isAdminOrManager = currentUser.getRole().name().equals("ADMIN") || currentUser.getRole().name().equals("MANAGER");

        if (!isOwner && !isAdminOrManager) {
            throw new ForbiddenException("You do not have permission to delete this review");
        }

        review.setStatus(ReviewStatus.DELETED);
        review.setDeletedAt(LocalDateTime.now());
        reviewRepository.save(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductReviewResponse> getProductReviews(Long productId, Integer ratingFilter, Pageable pageable) {
        getProductOrThrow(productId);

        Page<ProductReview> reviewPage;
        if (ratingFilter != null && ratingFilter >= 1 && ratingFilter <= 5) {
            reviewPage = reviewRepository.findByProductIdAndStatusAndRatingAndDeletedAtIsNull(productId, ReviewStatus.APPROVED, ratingFilter, pageable);
        } else {
            reviewPage = reviewRepository.findByProductIdAndStatusAndDeletedAtIsNull(productId, ReviewStatus.APPROVED, pageable);
        }

        return PageResponse.from(reviewPage, ProductReviewResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(Long productId) {
        getProductOrThrow(productId);

        Map<Integer, Long> starCounts = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            starCounts.put(i, 0L);
        }

        List<Object[]> results = reviewRepository.countReviewsGroupByRating(productId);
        long totalCount = 0L;
        double totalWeightedRating = 0.0;

        for (Object[] row : results) {
            Integer rating = (Integer) row[0];
            Long count = (Long) row[1];

            if (rating != null && rating >= 1 && rating <= 5) {
                starCounts.put(rating, count);
                totalCount += count;
                totalWeightedRating += (rating * count);
            }
        }

        double averageRating = totalCount > 0 ? Math.round((totalWeightedRating / totalCount) * 10.0) / 10.0 : 0.0;

        return RatingSummaryResponse.builder()
                .productId(productId)
                .averageRating(averageRating)
                .totalReviews(totalCount)
                .starCounts(starCounts)
                .build();
    }

    @Override
    @Transactional
    public ProductReviewResponse adminUpdateStatus(Long reviewId, ReviewStatus status) {
        ProductReview review = getReviewOrThrow(reviewId);
        review.setStatus(status);
        if (status == ReviewStatus.DELETED) {
            review.setDeletedAt(LocalDateTime.now());
        } else {
            review.setDeletedAt(null);
        }
        ProductReview updated = reviewRepository.save(review);
        return ProductReviewResponse.from(updated);
    }

    private Product getProductOrThrow(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }

    private User getUserOrThrow(String username) {
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User", "username/email", username));
    }

    private ProductReview getReviewOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductReview", "id", reviewId));
    }
}
