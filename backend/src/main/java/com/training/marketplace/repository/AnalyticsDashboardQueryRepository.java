package com.training.marketplace.repository;

import com.training.marketplace.analytics.AnalyticsCacheNames;
import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.dto.response.ProductPerformanceResponse;
import com.training.marketplace.dto.response.PromotionRecommendationPerformanceResponse;
import com.training.marketplace.dto.response.PromotionTrendPointResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AnalyticsDashboardQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    @Cacheable(cacheNames = AnalyticsCacheNames.OVERVIEW, sync = true)
    public AnalyticsOverviewResponse overview(AnalyticsDashboardFilter filter) {
        String sql = """
                WITH filtered AS (
                    SELECT ae.*
                      FROM analytics_events ae
                      LEFT JOIN products p ON p.id = ae.product_id
                     WHERE ae.occurred_at >= ? AND ae.occurred_at < ?
                       AND (CAST(? AS BIGINT) IS NULL OR p.category_id = ?)
                       AND (CAST(? AS BIGINT) IS NULL OR ae.product_id = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'campaign' = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.placement = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'deviceType' = ?)
                ),
                current_customers AS (
                    SELECT filtered.user_id,
                           MIN(COALESCE(orders.created_at,
                               filtered.occurred_at AT TIME ZONE 'UTC')) AS first_order_at
                      FROM filtered
                      LEFT JOIN orders ON orders.id = filtered.order_id
                     WHERE filtered.event_type = 'ORDER_CREATED'
                       AND filtered.user_id IS NOT NULL
                     GROUP BY filtered.user_id
                ),
                customer_status AS (
                    SELECT current_customers.user_id,
                           EXISTS (
                               SELECT 1
                                 FROM orders history
                                WHERE history.user_id = current_customers.user_id
                                  AND history.status <> 'CANCELLED'
                                  AND history.created_at < current_customers.first_order_at
                           ) AS is_returning
                      FROM current_customers
                )
                SELECT COUNT(*) FILTER (WHERE event_type = 'PRODUCT_VIEW') AS product_views,
                       COUNT(*) FILTER (WHERE event_type = 'ADD_TO_CART') AS add_to_carts,
                       COUNT(*) FILTER (WHERE event_type = 'BEGIN_CHECKOUT') AS begin_checkouts,
                       COUNT(*) FILTER (WHERE event_type = 'ORDER_CREATED') AS orders,
                       COALESCE((SELECT COUNT(*) FROM customer_status WHERE is_returning), 0) AS returning_customers,
                       COALESCE((SELECT COUNT(*) FROM customer_status), 0) AS ordering_customers,
                       MAX(received_at) AS last_updated_at
                  FROM filtered
                """;
        List<Object> args = filterArgs(filter);
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> new AnalyticsOverviewResponse(
                rs.getLong("product_views"),
                rs.getLong("add_to_carts"),
                rs.getLong("begin_checkouts"),
                rs.getLong("orders"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                rate(rs.getLong("returning_customers"), rs.getLong("ordering_customers")),
                instant(rs, "last_updated_at")), args.toArray());
    }

    @Cacheable(cacheNames = AnalyticsCacheNames.PRODUCT_PERFORMANCE, sync = true)
    public PageResponse<ProductPerformanceResponse> productPerformance(
            AnalyticsDashboardFilter filter, int page, int size) {
        return productPerformance(filter, page, size, null, "productViews", "desc");
    }

    @Cacheable(cacheNames = AnalyticsCacheNames.PRODUCT_PERFORMANCE, sync = true)
    public PageResponse<ProductPerformanceResponse> productPerformance(
            AnalyticsDashboardFilter filter, int page, int size,
            String search, String sortBy, String sortDirection) {
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        String orderColumn = switch (sortBy == null ? "" : sortBy) {
            case "productName" -> "product_name";
            case "wishlists" -> "wishlists";
            case "addToCarts" -> "add_to_carts";
            case "orders" -> "orders";
            case "averageRating" -> "average_rating";
            case "questionCount" -> "question_count";
            default -> "product_views";
        };
        String direction = "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        String sql = """
                WITH event_metrics AS (
                    SELECT ae.product_id,
                           COUNT(*) FILTER (WHERE ae.event_type = 'PRODUCT_VIEW') AS product_views,
                           COUNT(*) FILTER (WHERE ae.event_type = 'ADD_TO_WISHLIST') AS wishlists,
                           COUNT(*) FILTER (WHERE ae.event_type = 'ADD_TO_CART') AS add_to_carts,
                           MAX(ae.received_at) AS event_updated_at
                      FROM analytics_events ae
                     WHERE ae.occurred_at >= ? AND ae.occurred_at < ?
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'campaign' = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.placement = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'deviceType' = ?)
                       AND ae.product_id IS NOT NULL
                     GROUP BY ae.product_id
                ),
                attributed_order_metrics AS (
                    SELECT ae.product_id,
                           COUNT(DISTINCT ae.order_id) AS orders,
                           MAX(ae.received_at) AS order_updated_at
                      FROM analytics_events ae
                     WHERE ae.occurred_at >= ? AND ae.occurred_at < ?
                       AND ae.event_type IN ('ORDER_CREATED', 'PURCHASE')
                       AND ae.product_id IS NOT NULL
                       AND ae.order_id IS NOT NULL
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'campaign' = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.placement = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'deviceType' = ?)
                     GROUP BY ae.product_id
                ),
                order_metrics AS (
                    SELECT pv.product_id, COUNT(DISTINCT o.id) AS orders, MAX(o.updated_at) AS order_updated_at
                      FROM orders o
                      JOIN order_items oi ON oi.order_id = o.id
                      JOIN product_variants pv ON pv.id = oi.variant_id
                     WHERE o.created_at >= ? AND o.created_at < ?
                       AND o.status <> 'CANCELLED'
                     GROUP BY pv.product_id
                ),
                review_metrics AS (
                    SELECT product_id, AVG(rating) AS average_rating, MAX(updated_at) AS review_updated_at
                      FROM product_reviews
                     WHERE status = 'APPROVED' AND deleted_at IS NULL
                       AND created_at >= ? AND created_at < ?
                     GROUP BY product_id
                ),
                question_metrics AS (
                    SELECT product_id, COUNT(*) AS question_count, MAX(updated_at) AS question_updated_at
                      FROM product_questions
                     WHERE status = 'VISIBLE'
                       AND created_at >= ? AND created_at < ?
                     GROUP BY product_id
                )
                SELECT p.id AS product_id,
                       p.name AS product_name,
                       p.category_id,
                       COALESCE(em.product_views, 0) AS product_views,
                       COALESCE(em.wishlists, 0) AS wishlists,
                       COALESCE(em.add_to_carts, 0) AS add_to_carts,
                       CASE WHEN CAST(? AS BOOLEAN)
                            THEN COALESCE(aom.orders, 0)
                            ELSE COALESCE(om.orders, 0)
                        END AS orders,
                       COALESCE(rm.average_rating, 0) AS average_rating,
                       COALESCE(qm.question_count, 0) AS question_count,
                       GREATEST(em.event_updated_at, aom.order_updated_at, om.order_updated_at,
                                rm.review_updated_at, qm.question_updated_at) AS last_updated_at
                  FROM products p
                  LEFT JOIN event_metrics em ON em.product_id = p.id
                  LEFT JOIN attributed_order_metrics aom ON aom.product_id = p.id
                  LEFT JOIN order_metrics om ON om.product_id = p.id
                  LEFT JOIN review_metrics rm ON rm.product_id = p.id
                  LEFT JOIN question_metrics qm ON qm.product_id = p.id
                 WHERE (CAST(? AS BIGINT) IS NULL OR p.category_id = ?)
                   AND (CAST(? AS BIGINT) IS NULL OR p.id = ?)
                   AND (CAST(? AS VARCHAR) IS NULL OR LOWER(p.name) LIKE LOWER('%%' || ? || '%%'))
                 ORDER BY %s %s, p.id ASC
                 LIMIT ? OFFSET ?
                """.formatted(orderColumn, direction);
        List<Object> args = productPerformanceArgs(filter);
        args.add(hasAttributionFilter(filter));
        args.add(filter.categoryId());
        args.add(filter.categoryId());
        args.add(filter.productId());
        args.add(filter.productId());
        args.add(normalizedSearch);
        args.add(normalizedSearch);
        args.add(size);
        args.add((long) page * size);

        List<ProductPerformanceResponse> content = jdbcTemplate.query(sql, (rs, rowNum) ->
                new ProductPerformanceResponse(
                        rs.getLong("product_id"),
                        rs.getString("product_name"),
                        rs.getLong("category_id"),
                        rs.getLong("product_views"),
                        rs.getLong("wishlists"),
                        rs.getLong("add_to_carts"),
                        rs.getLong("orders"),
                        rs.getBigDecimal("average_rating"),
                        rs.getLong("question_count"),
                        instant(rs, "last_updated_at")), args.toArray());

        Long total = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM products p
                 WHERE (CAST(? AS BIGINT) IS NULL OR p.category_id = ?)
                   AND (CAST(? AS BIGINT) IS NULL OR p.id = ?)
                   AND (CAST(? AS VARCHAR) IS NULL OR LOWER(p.name) LIKE LOWER('%' || ? || '%'))
                """, Long.class, filter.categoryId(), filter.categoryId(), filter.productId(), filter.productId(),
                normalizedSearch, normalizedSearch);
        long totalElements = total == null ? 0 : total;
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages, page + 1 >= totalPages);
    }

    @Cacheable(cacheNames = AnalyticsCacheNames.PROMOTION_RECOMMENDATION, sync = true)
    public List<PromotionRecommendationPerformanceResponse> promotionRecommendationPerformance(
            AnalyticsDashboardFilter filter) {
        String sql = """
                WITH recommendation_metrics AS (
                    SELECT NULLIF(ae.properties ->> 'campaign', '') AS campaign,
                           ae.placement,
                           ae.strategy,
                           COUNT(*) FILTER (WHERE ae.event_type = 'RECOMMENDATION_IMPRESSION') AS impressions,
                           COUNT(*) FILTER (WHERE ae.event_type = 'RECOMMENDATION_CLICK') AS clicks,
                           COUNT(*) FILTER (
                               WHERE ae.event_type = 'ADD_TO_CART'
                                 AND ae.recommendation_request_id IS NOT NULL
                           ) AS add_to_carts,
                           COUNT(DISTINCT ae.order_id) FILTER (
                               WHERE ae.event_type = 'PURCHASE'
                                 AND ae.recommendation_request_id IS NOT NULL
                                 AND ae.order_id IS NOT NULL
                           ) AS attributed_orders,
                           MAX(ae.received_at) AS last_updated_at
                      FROM analytics_events ae
                      LEFT JOIN products p ON p.id = ae.product_id
                     WHERE ae.occurred_at >= ? AND ae.occurred_at < ?
                       AND ae.event_type IN (
                           'RECOMMENDATION_IMPRESSION', 'RECOMMENDATION_CLICK', 'ADD_TO_CART', 'PURCHASE'
                       )
                       AND (
                           ae.recommendation_request_id IS NOT NULL
                           OR NULLIF(ae.properties ->> 'campaign', '') IS NOT NULL
                       )
                       AND (CAST(? AS BIGINT) IS NULL OR p.category_id = ?)
                       AND (CAST(? AS BIGINT) IS NULL OR ae.product_id = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'campaign' = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.placement = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'deviceType' = ?)
                     GROUP BY NULLIF(ae.properties ->> 'campaign', ''), ae.placement, ae.strategy
                ),
                campaign_metrics AS (
                    SELECT c.name AS campaign,
                           'CAMPAIGN_STRIP' AS placement,
                           'MERCHANDISING' AS strategy,
                           COUNT(*) FILTER (WHERE me.event_type = 'IMPRESSION') AS impressions,
                           COUNT(*) FILTER (WHERE me.event_type = 'CLICK') AS clicks,
                           0::BIGINT AS add_to_carts,
                           COUNT(DISTINCT me.order_id) FILTER (WHERE me.order_id IS NOT NULL) AS attributed_orders,
                           MAX(me.received_at) AS last_updated_at
                      FROM merchandising_events me
                      JOIN campaigns c ON c.id = me.target_id
                     WHERE me.target_type = 'CAMPAIGN'
                       AND me.occurred_at >= ? AND me.occurred_at < ?
                       AND (CAST(? AS VARCHAR) IS NULL OR c.name = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ? = 'CAMPAIGN_STRIP')
                       AND CAST(? AS BIGINT) IS NULL
                       AND CAST(? AS BIGINT) IS NULL
                       AND CAST(? AS VARCHAR) IS NULL
                     GROUP BY c.name
                )
                SELECT * FROM recommendation_metrics
                UNION ALL
                SELECT * FROM campaign_metrics
                 ORDER BY impressions DESC, clicks DESC, campaign NULLS LAST,
                          placement NULLS LAST, strategy NULLS LAST
                """;
        List<Object> args = filterArgs(filter);
        args.add(localTimestamp(filter.from()));
        args.add(localTimestamp(filter.to()));
        args.add(filter.campaign());
        args.add(filter.campaign());
        args.add(filter.placement());
        args.add(filter.placement());
        args.add(filter.categoryId());
        args.add(filter.productId());
        args.add(filter.deviceType());
        return jdbcTemplate.query(sql, (rs, rowNum) ->
                new PromotionRecommendationPerformanceResponse(
                        rs.getString("campaign"),
                        rs.getString("placement"),
                        rs.getString("strategy"),
                        rs.getLong("impressions"),
                        rs.getLong("clicks"),
                        BigDecimal.ZERO,
                        rs.getLong("add_to_carts"),
                        rs.getLong("attributed_orders"),
                instant(rs, "last_updated_at")), args.toArray());
    }

    @Cacheable(cacheNames = AnalyticsCacheNames.PROMOTION_TREND, sync = true)
    public List<PromotionTrendPointResponse> promotionTrend(AnalyticsDashboardFilter filter) {
        String sql = """
                WITH trend_rows AS (
                    SELECT CAST(ae.occurred_at AS date) AS event_date,
                           COUNT(*) FILTER (WHERE ae.event_type = 'RECOMMENDATION_IMPRESSION') AS impressions,
                           COUNT(*) FILTER (WHERE ae.event_type = 'RECOMMENDATION_CLICK') AS clicks,
                           COUNT(*) FILTER (WHERE ae.event_type = 'ADD_TO_CART') AS add_to_carts,
                           COUNT(DISTINCT ae.order_id) FILTER (WHERE ae.event_type = 'PURCHASE') AS attributed_orders
                      FROM analytics_events ae
                     WHERE ae.occurred_at >= ? AND ae.occurred_at < ?
                       AND ae.recommendation_request_id IS NOT NULL
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'campaign' = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.placement = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ae.properties ->> 'deviceType' = ?)
                     GROUP BY CAST(ae.occurred_at AS date)
                    UNION ALL
                    SELECT CAST(me.occurred_at AS date) AS event_date,
                           COUNT(*) FILTER (WHERE me.event_type = 'IMPRESSION') AS impressions,
                           COUNT(*) FILTER (WHERE me.event_type = 'CLICK') AS clicks,
                           0::BIGINT AS add_to_carts,
                           COUNT(DISTINCT me.order_id) FILTER (WHERE me.order_id IS NOT NULL) AS attributed_orders
                      FROM merchandising_events me
                      JOIN campaigns c ON c.id = me.target_id
                     WHERE me.target_type = 'CAMPAIGN'
                       AND me.occurred_at >= ? AND me.occurred_at < ?
                       AND (CAST(? AS VARCHAR) IS NULL OR c.name = ?)
                       AND (CAST(? AS VARCHAR) IS NULL OR ? = 'CAMPAIGN_STRIP')
                       AND CAST(? AS VARCHAR) IS NULL
                     GROUP BY CAST(me.occurred_at AS date)
                )
                SELECT event_date,
                       SUM(impressions) AS impressions,
                       SUM(clicks) AS clicks,
                       SUM(add_to_carts) AS add_to_carts,
                       SUM(attributed_orders) AS attributed_orders
                  FROM trend_rows
                 GROUP BY event_date
                 ORDER BY event_date
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new PromotionTrendPointResponse(
                rs.getDate("event_date").toLocalDate(),
                rs.getLong("impressions"), rs.getLong("clicks"),
                rs.getLong("add_to_carts"), rs.getLong("attributed_orders")),
                Timestamp.from(filter.from()), Timestamp.from(filter.to()),
                filter.campaign(), filter.campaign(), filter.placement(), filter.placement(),
                filter.deviceType(), filter.deviceType(),
                localTimestamp(filter.from()), localTimestamp(filter.to()),
                filter.campaign(), filter.campaign(), filter.placement(), filter.placement(),
                filter.deviceType());
    }

    private List<Object> productPerformanceArgs(AnalyticsDashboardFilter filter) {
        List<Object> args = new ArrayList<>();
        addEventFilterArgs(args, filter);
        addEventFilterArgs(args, filter);
        args.add(Timestamp.from(filter.from()));
        args.add(Timestamp.from(filter.to()));
        args.add(Timestamp.from(filter.from()));
        args.add(Timestamp.from(filter.to()));
        args.add(Timestamp.from(filter.from()));
        args.add(Timestamp.from(filter.to()));
        return args;
    }

    private void addEventFilterArgs(List<Object> args, AnalyticsDashboardFilter filter) {
        args.add(Timestamp.from(filter.from()));
        args.add(Timestamp.from(filter.to()));
        args.add(filter.campaign());
        args.add(filter.campaign());
        args.add(filter.placement());
        args.add(filter.placement());
        args.add(filter.deviceType());
        args.add(filter.deviceType());
    }

    private boolean hasAttributionFilter(AnalyticsDashboardFilter filter) {
        return filter.campaign() != null || filter.placement() != null || filter.deviceType() != null;
    }

    private Timestamp localTimestamp(Instant instant) {
        return Timestamp.valueOf(LocalDateTime.ofInstant(instant, ZoneOffset.UTC));
    }

    private List<Object> filterArgs(AnalyticsDashboardFilter filter) {
        List<Object> args = new ArrayList<>();
        args.add(Timestamp.from(filter.from()));
        args.add(Timestamp.from(filter.to()));
        args.add(filter.categoryId());
        args.add(filter.categoryId());
        args.add(filter.productId());
        args.add(filter.productId());
        args.add(filter.campaign());
        args.add(filter.campaign());
        args.add(filter.placement());
        args.add(filter.placement());
        args.add(filter.deviceType());
        args.add(filter.deviceType());
        return args;
    }

    public static BigDecimal rate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(numerator)
                .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private Instant instant(ResultSet rs, String column) throws java.sql.SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toInstant();
    }
}
