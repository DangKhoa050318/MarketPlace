package com.training.marketplace.entity;

import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.analytics.AnalyticsExportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analytics_export_jobs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsExportJob extends BaseEntity {

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "export_type", nullable = false, length = 40)
    private AnalyticsExportType exportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalyticsExportStatus status;

    @Column(name = "from_time", nullable = false)
    private Instant from;

    @Column(name = "to_time", nullable = false)
    private Instant to;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "product_id")
    private Long productId;

    @Column(length = 120)
    private String campaign;

    @Column(length = 80)
    private String placement;

    @Column(name = "device_type", length = 40)
    private String deviceType;

    @Column(name = "file_name", length = 180)
    private String fileName;

    @Column(name = "csv_content", columnDefinition = "TEXT")
    private String csvContent;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
