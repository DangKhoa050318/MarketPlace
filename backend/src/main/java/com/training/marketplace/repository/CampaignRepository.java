package com.training.marketplace.repository;

import com.training.marketplace.entity.Campaign;
import com.training.marketplace.enums.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    @Query("""
            SELECT c FROM Campaign c
            WHERE (:status IS NULL OR c.status = :status)
              AND (:q IS NULL OR UPPER(c.name) LIKE UPPER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<Campaign> search(@Param("status") CampaignStatus status,
                          @Param("q") String q,
                          Pageable pageable);

    /** Effective campaigns: PUBLISHED + active + now within [startsAt, endsAt] (B-405). */
    @Query("""
            SELECT c FROM Campaign c
            WHERE c.status = com.training.marketplace.enums.CampaignStatus.PUBLISHED
              AND c.active = true
              AND (c.startsAt IS NULL OR c.startsAt <= :now)
              AND (c.endsAt   IS NULL OR c.endsAt   >= :now)
            ORDER BY c.startsAt DESC
            """)
    List<Campaign> findEffective(@Param("now") LocalDateTime now);
}
