package com.training.marketplace.repository;

import com.training.marketplace.entity.AnalyticsEvent;
import com.training.marketplace.repository.projection.ProductCoOccurrenceProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    Optional<AnalyticsEvent> findByEventId(UUID eventId);

    boolean existsByEventId(UUID eventId);

    @Query(value = """
            WITH distinct_views AS (
                SELECT product_id,
                       COALESCE(
                           's:' || NULLIF(BTRIM(session_id), ''),
                           'u:' || CAST(user_id AS text)
                       ) AS actor_key
                  FROM analytics_events
                 WHERE event_type = 'PRODUCT_VIEW'
                   AND occurred_at >= :since
                   AND product_id IS NOT NULL
                   AND (NULLIF(BTRIM(session_id), '') IS NOT NULL OR user_id IS NOT NULL)
                 GROUP BY product_id,
                          COALESCE(
                              's:' || NULLIF(BTRIM(session_id), ''),
                              'u:' || CAST(user_id AS text)
                          )
            )
            SELECT candidate.product_id AS "productId",
                   COUNT(*) AS "coOccurrenceCount"
              FROM distinct_views source
              JOIN distinct_views candidate
                ON candidate.actor_key = source.actor_key
               AND candidate.product_id <> source.product_id
              JOIN products candidate_product
                ON candidate_product.id = candidate.product_id
               AND candidate_product.active = TRUE
             WHERE source.product_id = :sourceProductId
               AND EXISTS (
                   SELECT 1
                     FROM product_variants candidate_variant
                    WHERE candidate_variant.product_id = candidate.product_id
                      AND candidate_variant.active = TRUE
               )
             GROUP BY candidate.product_id
            HAVING COUNT(*) >= :minOccurrences
             ORDER BY COUNT(*) DESC, candidate.product_id ASC
            """, nativeQuery = true)
    List<ProductCoOccurrenceProjection> findCoViewedProducts(
            @Param("sourceProductId") Long sourceProductId,
            @Param("since") Instant since,
            @Param("minOccurrences") int minOccurrences,
            Pageable pageable);
}
