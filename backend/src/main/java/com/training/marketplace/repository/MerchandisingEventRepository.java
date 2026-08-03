package com.training.marketplace.repository;

import com.training.marketplace.entity.MerchandisingEvent;
import com.training.marketplace.enums.MerchandisingEventType;
import com.training.marketplace.enums.MerchandisingTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MerchandisingEventRepository extends JpaRepository<MerchandisingEvent, Long> {

    /** Idempotent dedup: an event whose client eventId already exists is a duplicate (B-407). */
    boolean existsByEventId(UUID eventId);

    /** Count impressions/clicks for one target within [from, to) — used by the summary (B-408). */
    @Query("""
            SELECT COUNT(e) FROM MerchandisingEvent e
            WHERE e.targetType = :targetType AND e.targetId = :targetId
              AND e.eventType = :eventType
              AND e.occurredAt >= :from AND e.occurredAt < :to
            """)
    long countInRange(@Param("targetType") MerchandisingTargetType targetType,
                      @Param("targetId") Long targetId,
                      @Param("eventType") MerchandisingEventType eventType,
                      @Param("from") LocalDateTime from,
                      @Param("to") LocalDateTime to);

    /** Distinct orders attributed to one target within [from, to) — used by the summary (B-408). */
    @Query("""
            SELECT COUNT(DISTINCT e.orderId) FROM MerchandisingEvent e
            WHERE e.targetType = :targetType AND e.targetId = :targetId
              AND e.orderId IS NOT NULL
              AND e.occurredAt >= :from AND e.occurredAt < :to
            """)
    long countAttributedOrders(@Param("targetType") MerchandisingTargetType targetType,
                               @Param("targetId") Long targetId,
                               @Param("from") LocalDateTime from,
                               @Param("to") LocalDateTime to);

    /** A user's not-yet-attributed CLICK events within [since, until], most recent first (B-408). */
    @Query("""
            SELECT e FROM MerchandisingEvent e
            WHERE e.eventType = com.training.marketplace.enums.MerchandisingEventType.CLICK
              AND e.userId = :userId
              AND e.orderId IS NULL
              AND e.occurredAt >= :since AND e.occurredAt <= :until
            ORDER BY e.occurredAt DESC
            """)
    List<MerchandisingEvent> findRecentUnattributedClicksByUser(@Param("userId") Long userId,
                                                                @Param("since") LocalDateTime since,
                                                                @Param("until") LocalDateTime until);
}
