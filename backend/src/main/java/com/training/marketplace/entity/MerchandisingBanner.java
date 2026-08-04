package com.training.marketplace.entity;

import com.training.marketplace.enums.PublishStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "merchandising_banners")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchandisingBanner extends BaseEntity {

    @Column(nullable = false, length = 150)
    private String title;

    @Column(name = "image_url_desktop", nullable = false, length = 500)
    private String imageUrlDesktop;

    @Column(name = "image_url_mobile", length = 500)
    private String imageUrlMobile;

    @Column(name = "alt_text", nullable = false, length = 255)
    private String altText;

    @Column(name = "target_url", length = 500)
    private String targetUrl;

    @Column(nullable = false, length = 40)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private PublishStatus status = PublishStatus.DRAFT;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Version
    private Long version;
}