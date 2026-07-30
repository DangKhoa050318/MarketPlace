package com.training.marketplace.repository;

import com.training.marketplace.entity.AnalyticsEvent;
import com.training.marketplace.repository.projection.ProductCoOccurrenceProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query(value = """
            UPDATE analytics_events
               SET user_id = :userId
             WHERE session_id = :sessionId
               AND user_id IS NULL
            """, nativeQuery = true)
    int linkAnonymousSessionToUser(@Param("sessionId") String sessionId, @Param("userId") Long userId);

    @Modifying
    @Query(value = """
            UPDATE analytics_events
               SET user_id = NULL,
                   session_id = NULL,
                   properties = properties - 'email' - 'username' - 'fullName' - 'ipAddress'
             WHERE occurred_at < :cutoff
               AND (user_id IS NOT NULL OR session_id IS NOT NULL)
            """, nativeQuery = true)
    int anonymizeExpiredRawEvents(@Param("cutoff") Instant cutoff);

    @Query(value = """
            WITH filtered AS (
                SELECT event_type,
                       COALESCE('u:' || user_id::text, 's:' || NULLIF(BTRIM(session_id), '')) AS actor_key,
                       occurred_at
                  FROM analytics_events ae
                  LEFT JOIN products p ON p.id = ae.product_id
                 WHERE occurred_at >= :from
                   AND occurred_at < :to
                   AND event_type IN ('PRODUCT_VIEW', 'ADD_TO_CART', 'BEGIN_CHECKOUT', 'ORDER_CREATED')
                   AND COALESCE('u:' || user_id::text, 's:' || NULLIF(BTRIM(session_id), '')) IS NOT NULL
                   AND (:categoryId IS NULL OR p.category_id = :categoryId)
                   AND (:productId IS NULL OR ae.product_id = :productId)
                   AND (:campaign IS NULL OR ae.properties ->> 'campaign' = :campaign)
                   AND (:deviceType IS NULL OR ae.properties ->> 'deviceType' = :deviceType)
            ),
            firsts AS (
                SELECT actor_key,
                       MIN(occurred_at) FILTER (WHERE event_type = 'PRODUCT_VIEW') AS product_view_at,
                       MIN(occurred_at) FILTER (WHERE event_type = 'ADD_TO_CART') AS add_to_cart_at,
                       MIN(occurred_at) FILTER (WHERE event_type = 'BEGIN_CHECKOUT') AS begin_checkout_at,
                       MIN(occurred_at) FILTER (WHERE event_type = 'ORDER_CREATED') AS order_created_at
                  FROM filtered
                 GROUP BY actor_key
            )
            SELECT
                COUNT(*) FILTER (WHERE product_view_at IS NOT NULL) AS productViews,
                COUNT(*) FILTER (WHERE product_view_at IS NOT NULL AND add_to_cart_at >= product_view_at) AS addToCarts,
                COUNT(*) FILTER (
                    WHERE product_view_at IS NOT NULL
                      AND add_to_cart_at >= product_view_at
                      AND begin_checkout_at >= add_to_cart_at
                ) AS beginCheckouts,
                COUNT(*) FILTER (
                    WHERE product_view_at IS NOT NULL
                      AND add_to_cart_at >= product_view_at
                      AND begin_checkout_at >= add_to_cart_at
                      AND order_created_at >= begin_checkout_at
                ) AS orderCreated
              FROM firsts
            """, nativeQuery = true)
    FunnelCounts summarizeFunnel(
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("categoryId") Long categoryId,
            @Param("productId") Long productId,
            @Param("campaign") String campaign,
            @Param("deviceType") String deviceType);

    interface FunnelCounts {
        long getProductViews();

        long getAddToCarts();

        long getBeginCheckouts();

        long getOrderCreated();
    }
}
