package com.training.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String actor;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "target_type", nullable = false, length = 100)
    private String targetType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String details;
}
