package com.training.marketplace.entity;

import com.training.marketplace.enums.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
// Uniqueness is enforced in the database by the partial unique index `uk_user_order_item`
// on (user_id, order_item_id) WHERE order_item_id IS NOT NULL — see migration
// V20260803150000__allow_review_per_order_item.sql (the older uk_user_product was dropped there).
// A JPA @UniqueConstraint cannot express a partial index, so it is intentionally not declared here;
// ddl-auto=validate does not verify unique constraints, so the entity stays in sync with the DB.
@Table(name = "product_reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductReview extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.APPROVED;

    @Column(name = "is_verified_purchase", nullable = false)
    @Builder.Default
    private Boolean isVerifiedPurchase = false;

    @Column(name = "is_edited", nullable = false)
    @Builder.Default
    private Boolean isEdited = false;

    @Column(name = "helpful_count", nullable = false)
    @Builder.Default
    private Long helpfulCount = 0L;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /** Optional shop/seller reply to this review (G4). */
    @Column(name = "seller_reply", columnDefinition = "TEXT")
    private String sellerReply;

    @Column(name = "seller_reply_at")
    private LocalDateTime sellerReplyAt;
}
