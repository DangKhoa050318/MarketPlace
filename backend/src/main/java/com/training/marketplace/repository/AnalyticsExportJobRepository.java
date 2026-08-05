package com.training.marketplace.repository;

import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.entity.AnalyticsExportJob;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalyticsExportJobRepository extends JpaRepository<AnalyticsExportJob, Long> {

    Optional<AnalyticsExportJob> findByPublicId(UUID publicId);

    List<AnalyticsExportJob> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            AnalyticsExportStatus status, Instant nextAttemptAt);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AnalyticsExportJob job
               set job.status = com.training.marketplace.analytics.AnalyticsExportStatus.PROCESSING,
                   job.attemptCount = job.attemptCount + 1,
                   job.startedAt = :startedAt,
                   job.nextAttemptAt = null
             where job.publicId = :publicId
               and job.status = com.training.marketplace.analytics.AnalyticsExportStatus.PENDING
            """)
    int claim(@Param("publicId") UUID publicId, @Param("startedAt") Instant startedAt);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AnalyticsExportJob job
               set job.status = com.training.marketplace.analytics.AnalyticsExportStatus.PENDING,
                   job.nextAttemptAt = :retryAt,
                   job.startedAt = null
             where job.status = com.training.marketplace.analytics.AnalyticsExportStatus.PROCESSING
               and job.startedAt < :staleBefore
            """)
    int recoverStuck(@Param("staleBefore") Instant staleBefore, @Param("retryAt") Instant retryAt);
}
