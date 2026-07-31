package com.training.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "product_collection_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCollectionItem extends BaseEntity {

    @Column(name = "collection_id", nullable = false)
    private Long collectionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}