package com.training.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "anonymous_wishlist_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_anonymous_wishlist_session_product",
                columnNames = {"session_id", "product_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnonymousWishlistItem extends BaseEntity {

    @Column(name = "session_id", nullable = false, length = 128)
    private String sessionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;
}
