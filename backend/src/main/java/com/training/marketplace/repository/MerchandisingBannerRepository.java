package com.training.marketplace.repository;

import com.training.marketplace.entity.MerchandisingBanner;
import com.training.marketplace.enums.PublishStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MerchandisingBannerRepository extends JpaRepository<MerchandisingBanner, Long> {

    @Query("""
            SELECT b FROM MerchandisingBanner b
            WHERE (:status IS NULL OR b.status = :status)
              AND (:position IS NULL OR b.position = :position)
            ORDER BY b.position ASC, b.displayOrder ASC
            """)
    Page<MerchandisingBanner> search(@Param("status") PublishStatus status,
                                     @Param("position") String position,
                                     Pageable pageable);

    /** Effective banners in a slot: PUBLISHED + active + now within window (B-405), ordered. */
    @Query("""
            SELECT b FROM MerchandisingBanner b
            WHERE b.status = com.training.marketplace.enums.PublishStatus.PUBLISHED
              AND b.active = true
              AND b.position = :position
              AND (b.startsAt IS NULL OR b.startsAt <= :now)
              AND (b.endsAt   IS NULL OR b.endsAt   >= :now)
            ORDER BY b.displayOrder ASC
            """)
    List<MerchandisingBanner> findEffectiveByPosition(@Param("position") String position,
                                                      @Param("now") LocalDateTime now);
}
