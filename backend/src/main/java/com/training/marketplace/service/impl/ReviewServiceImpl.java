package com.training.marketplace.service.impl;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateReviewRequest;
import com.training.marketplace.dto.request.UpdateReviewRequest;
import com.training.marketplace.dto.response.ProductRatingSummaryResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;

    /** G5: a delivered order is reviewable only within this many days of delivery. */
    @Value("${marketplace.review.window-days:90}")
    private long reviewWindowDays;

    @Override
    @Transactional(readOnly = true)
    public ReviewEligibilityResponse checkEligibility(Long productId, String username) {
        Product product = getProductOrThrow(productId);
        User user = getUserOrThrow(username);

        Optional<ProductReview> existingReviewOpt = reviewRepository.findByUserIdAndProductIdAndDeletedAtIsNull(user.getId(), product.getId());
        ProductReviewResponse existingResponse = existingReviewOpt.map(ProductReviewResponse::from).orElse(null);

        LocalDateTime since = LocalDateTime.now().minusDays(reviewWindowDays);
        List<OrderItem> eligibleItems = orderItemRepository.findEligibleOrderItemsForReview(user.getId(), product.getId(), since);
        boolean isVerifiedPurchase = !eligibleItems.isEmpty();

        // Eligible only if there is a delivered purchase that has not been reviewed yet.
        Optional<OrderItem> reviewableItem = eligibleItems.stream()
                .filter(item -> !reviewRepository.existsByUserIdAndOrderItemIdAndDeletedAtIsNull(user.getId(), item.getId()))
                .findFirst();

        if (!isVerifiedPurchase) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .isVerifiedPurchase(false)
                    .orderItemId(null)
                    .existingReview(existingResponse)
                    .message("You can only review products you have purchased and received.")
                    .build();
        }

        if (reviewableItem.isEmpty()) {
            return ReviewEligibilityResponse.builder()
                    .eligible(false)
                    .isVerifiedPurchase(true)
                    .orderItemId(null)
                    .existingReview(existingResponse)
                    .message("You have already reviewed your purchase of this product.")
                    .build();
        }

        return ReviewEligibilityResponse.builder()
                .eligible(true)
                .isVerifiedPurchase(true)
                .orderItemId(reviewableItem.get().getId())
                .existingReview(existingResponse)
                .message("Eligible to write a verified purchase review.")
                .build();
    }

    @Override
    @Transactional
    public ProductReviewResponse createReview(Long productId, String username, CreateReviewRequest request) {
        Product product = getProductOrThrow(productId);
        User user = getUserOrThrow(username);

        // Anti-fake-review gate (FEATURE-03): only a customer who purchased this product AND
        // received it (order DELIVERED) may review. Every review is therefore a verified purchase.
        LocalDateTime since = LocalDateTime.now().minusDays(reviewWindowDays);
        List<OrderItem> eligibleItems = orderItemRepository.findEligibleOrderItemsForReview(user.getId(), product.getId(), since);
        if (eligibleItems.isEmpty()) {
            throw new ForbiddenException(
                    "You can only review a product you have purchased and received (order delivered) within the last "
                            + reviewWindowDays + " days.");
        }

        OrderItem orderItem;
        if (request.orderItemId() != null) {
            // The requested order item must be one of this user's delivered purchases of this product.
            orderItem = eligibleItems.stream()
                    .filter(item -> item.getId().equals(request.orderItemId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("The selected order item is not eligible to review this product."));
            if (reviewRepository.existsByUserIdAndOrderItemIdAndDeletedAtIsNull(user.getId(), orderItem.getId())) {
                throw new DuplicateResourceException("Review", "order_item_id", orderItem.getId());
            }
        } else {
            // Auto-pick the first delivered purchase that has not been reviewed yet.
            orderItem = eligibleItems.stream()
                    .filter(item -> !reviewRepository.existsByUserIdAndOrderItemIdAndDeletedAtIsNull(user.getId(), item.getId()))
                    .findFirst()
                    .orElseThrow(() -> new DuplicateResourceException("Review", "product_id", product.getId()));
        }

        String sanitizedTitle = request.title() != null ? request.title().trim() : "";
        String sanitizedContent = request.content() != null ? request.content().trim() : "";

        ProductReview review = ProductReview.builder()
                .user(user)
                .product(product)
                .orderItem(orderItem)
                .rating(request.rating())
                .title(sanitizedTitle)
                .content(sanitizedContent)
                .imageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null)
                .status(ReviewStatus.APPROVED)
                .isVerifiedPurchase(true)
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

        // G6: a customer may edit their own review only once (admins/managers are unrestricted).
        if (isOwner && !isAdminOrManager && Boolean.TRUE.equals(review.getIsEdited())) {
            throw new BadRequestException("You can only edit your review once.");
        }

        review.setRating(request.rating());
        review.setTitle(request.title() != null ? request.title().trim() : "");
        review.setContent(request.content() != null ? request.content().trim() : "");
        review.setImageUrl(request.imageUrl() != null ? request.imageUrl().trim() : null);
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
    public PageResponse<ProductReviewResponse> getReviewsForAdmin(Integer rating, ReviewStatus status, Long productId, Pageable pageable) {
        // Base scope: active reviews only (exclude soft-deleted). Filters are additive and all optional.
        Specification<ProductReview> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));

        if (rating != null && rating >= 1 && rating <= 5) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("rating"), rating));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (productId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId));
        }

        Page<ProductReview> reviewPage = reviewRepository.findAll(spec, pageable);
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
    @Transactional(readOnly = true)
    public List<ProductRatingSummaryResponse> getRatingSummaries(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        List<ProductRatingSummaryResponse> result = new ArrayList<>();
        for (Object[] row : reviewRepository.aggregateRatingsByProductIds(productIds)) {
            Long pid = (Long) row[0];
            double avg = row[1] != null ? Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0 : 0.0;
            long count = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            result.add(new ProductRatingSummaryResponse(pid, avg, count));
        }
        return result;
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
        ProductReview saved = reviewRepository.save(review);
        log.info("Admin updated review {} status to {}", reviewId, status);
        return ProductReviewResponse.from(saved);
    }

    @Override
    @Transactional
    public ProductReviewResponse adminReply(Long reviewId, String reply) {
        ProductReview review = getReviewOrThrow(reviewId);
        review.setSellerReply(reply.trim());
        review.setSellerReplyAt(LocalDateTime.now());
        ProductReview saved = reviewRepository.save(review);
        log.info("Admin/seller replied to review {}", reviewId);
        return ProductReviewResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMyReviewedProductIds(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User username: " + username));
        return reviewRepository.findReviewedProductIdsByUserId(user.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getMyReviewedOrderItemIds(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User username: " + username));
        return reviewRepository.findReviewedOrderItemIdsByUserId(user.getId());
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
