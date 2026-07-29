package com.training.marketplace.entity;

import com.training.marketplace.enums.ScopeRefType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One product-id or category-id inside a {@link PromotionCode}'s scope (B-303, extensible).
 * Child/detail entity: own {@code id}, plain {@code Long} FK, no {@code BaseEntity} timestamps.
 */
@Entity
@Table(name = "promotion_code_scopes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PromotionCodeScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "promotion_code_id", nullable = false)
    private Long promotionCodeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false, length = 10)
    private ScopeRefType refType;

    @Column(name = "ref_id", nullable = false)
    private Long refId;
}
