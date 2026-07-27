package com.training.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * SKU (the sellable unit / "mẫu mã") of a {@link Product}. Carries its own sku, price and
 * stock-control settings. Stock quantities themselves live per (variant, warehouse) in
 * {@code stock_levels}. {@code productId} is a plain {@code Long} foreign key.
 */
@Entity
@Table(name = "product_variants",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_variant_product_name",
                columnNames = {"product_id", "variant_name"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "variant_name", nullable = false, length = 255)
    private String variantName;

    @Column(length = 50)
    private String color;

    @Column(length = 50)
    private String size;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "min_stock", nullable = false)
    @Builder.Default
    private Integer minStock = 10;

    @Column(name = "max_stock", nullable = false)
    @Builder.Default
    private Integer maxStock = 1000;

    @Column(name = "reorder_point", nullable = false)
    @Builder.Default
    private Integer reorderPoint = 20;

    @Column(name = "reorder_quantity", nullable = false)
    @Builder.Default
    private Integer reorderQuantity = 100;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;
}
