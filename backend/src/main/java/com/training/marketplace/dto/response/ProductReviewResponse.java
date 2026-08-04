package com.training.marketplace.dto.response;

import com.training.marketplace.entity.ProductReview;
import com.training.marketplace.enums.ReviewStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductReviewResponse {
    private Long id;
    private Long userId;
    private String username;
    private String userFullName;
    private Long productId;
    private String productName;
    private Long orderItemId;
    private Integer rating;
    private String title;
    private String content;
    private ReviewStatus status;
    private Boolean isVerifiedPurchase;
    private Boolean isEdited;
    private Long helpfulCount;
    private String imageUrl;
    private String variantName;
    private String sku;
    private String sellerReply;
    private LocalDateTime sellerReplyAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductReviewResponse from(ProductReview review) {
        return ProductReviewResponse.builder()
                .id(review.getId())
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .username(review.getUser() != null ? review.getUser().getUsername() : null)
                .userFullName(review.getUser() != null ? review.getUser().getFullName() : null)
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .productName(review.getProduct() != null ? review.getProduct().getName() : null)
                .orderItemId(review.getOrderItem() != null ? review.getOrderItem().getId() : null)
                .rating(review.getRating())
                .title(review.getTitle())
                .content(review.getContent())
                .status(review.getStatus())
                .isVerifiedPurchase(review.getIsVerifiedPurchase())
                .isEdited(review.getIsEdited())
                .helpfulCount(review.getHelpfulCount())
                .imageUrl(review.getImageUrl())
                .variantName(review.getOrderItem() != null ? review.getOrderItem().getVariantName() : null)
                .sku(review.getOrderItem() != null ? review.getOrderItem().getSku() : null)
                .sellerReply(review.getSellerReply())
                .sellerReplyAt(review.getSellerReplyAt())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
