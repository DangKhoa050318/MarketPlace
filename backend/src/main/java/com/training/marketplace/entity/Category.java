package com.training.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Merged category: {@code code} (internal, from StockPulse) + {@code slug} (URL, from OrderFlow)
 * + {@code parentId} (self-referencing hierarchy). Foreign key kept as a plain {@code Long}.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Category extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "parent_id")
    private Long parentId;
}
