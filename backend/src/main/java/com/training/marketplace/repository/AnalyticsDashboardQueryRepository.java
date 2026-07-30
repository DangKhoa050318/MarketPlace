package com.training.marketplace.repository;

import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.dto.response.ProductPerformanceResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class AnalyticsDashboardQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsOverviewResponse overview(AnalyticsDashboardFilter filter) {
        String sql = """
                WITH filtered AS (
                    SELECT ae.*
                      FROM analytics_events ae
                      LEFT JOIN products p ON p.id = ae.product_id
                     WHERE ae.occurred_at >= ? AND ae.occurred_at < ?
                       AND (? IS NULL OR p.category_id = ?)
                       AND (? IS NULL OR ae.product_id = ?)
                       AND (? IS NULL OR ae.properties ->> 'campaign' = ?)
                       AND (? IS NULL OR ae.placement = ?)
                       AND (? IS NULL OR ae.properties ->> 'deviceType' = ?)
                ),
                customers AS (
                    SELECT user_id, COUNT(*) order_count
                      FROM orders
                     WHERE created_at >= ? AND created_at < ?
                       AND user_id IS NOT NULL
                     GROUP BY user_id
                )
                SELECT COUNT(*) FILTER (WHERE event_type = 'PRODUCT_VIEW') AS product_views,
                       COUNT(*) FILTER (WHERE event_type = 'ADD_TO_CART') AS add_to_carts,
                       COUNT(*) FILTER (WHERE event_type = 'BEGIN_CHECKOUT') AS begin_checkouts,
                       COUNT(*) FILTER (WHERE event_type = 'ORDER_CREATED') AS orders,
                       COALESCE((SELECT COUNT(*) FROM customers WHERE order_count > 1), 0) AS returning_customers,
                       COALESCE((SELECT COUNT(*) FROM customers), 0) AS ordering_customers,
                       MAX(received_at) AS last_updated_at
                  FROM filtered
                """;
        List<Object> args = filterArgs(filter);
        args.add(Timestamp.from(filter.from()));
        args.add(Timestamp.from(filter.to()));
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

    public PageResponse<ProductPerformanceResponse> productPerformance(
            AnalyticsDashboardFilter filter, int page, int size) {
        String sql = """
                WITH event_metrics AS (
                    SELECT ae.product_id,
                           COUNT(*) FILTER (WHERE ae.event_type = 'PRODUCT_VIEW') AS product_views,
                           COUNT(*) FILTER (WHERE ae.event_type = 'ADD_TO_WISHLIST') AS wishlists,
                           COUNT(*) FILTER (WHERE ae.event_type = 'ADD_TO_CART') AS add_to_carts,
                           MAX(ae.received_at) AS event_updated_at
                      FROM analytics_events ae
                     WHERE ae.occurred_at >= ? AND ae.occurred_at < ?
                       AND (? IS NULL OR ae.properties ->> 'campaign' = ?)
                       AND (? IS NULL OR ae.placement = ?)
                       AND (? IS NULL OR ae.properties ->> 'deviceType' = ?)
                       AND ae.product_id IS NOT NULL
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
                     GROUP BY product_id
                ),
                question_metrics AS (
                    SELECT product_id, COUNT(*) AS question_count, MAX(updated_at) AS question_updated_at
                      FROM product_questions
                     WHERE status = 'VISIBLE'
                     GROUP BY product_id
                )
                SELECT p.id AS product_id,
                       p.name AS product_name,
                       p.category_id,
                       COALESCE(em.product_views, 0) AS product_views,
                       COALESCE(em.wishlists, 0) AS wishlists,
                       COALESCE(em.add_to_carts, 0) AS add_to_carts,
                       COALESCE(om.orders, 0) AS orders,
                       COALESCE(rm.average_rating, 0) AS average_rating,
                       COALESCE(qm.question_count, 0) AS question_count,
                       GREATEST(em.event_updated_at, om.order_updated_at,
                                rm.review_updated_at, qm.question_updated_at) AS last_updated_at
                  FROM products p
                  LEFT JOIN event_metrics em ON em.product_id = p.id
                  LEFT JOIN order_metrics om ON om.product_id = p.id
                  LEFT JOIN review_metrics rm ON rm.product_id = p.id
                  LEFT JOIN question_metrics qm ON qm.product_id = p.id
                 WHERE (? IS NULL OR p.category_id = ?)
                   AND (? IS NULL OR p.id = ?)
                 ORDER BY product_views DESC, add_to_carts DESC, p.id ASC
                 LIMIT ? OFFSET ?
                """;
        List<Object> args = productPerformanceArgs(filter);
        args.add(filter.categoryId());
        args.add(filter.categoryId());
        args.add(filter.productId());
        args.add(filter.productId());
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
                 WHERE (? IS NULL OR p.category_id = ?)
                   AND (? IS NULL OR p.id = ?)
                """, Long.class, filter.categoryId(), filter.categoryId(), filter.productId(), filter.productId());
        long totalElements = total == null ? 0 : total;
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages, page + 1 >= totalPages);
    }

    private List<Object> productPerformanceArgs(AnalyticsDashboardFilter filter) {
        List<Object> args = new ArrayList<>();
        args.add(Timestamp.from(filter.from()));
        args.add(Timestamp.from(filter.to()));
        args.add(filter.campaign());
        args.add(filter.campaign());
        args.add(filter.placement());
        args.add(filter.placement());
        args.add(filter.deviceType());
        args.add(filter.deviceType());
        args.add(Timestamp.from(filter.from()));
        args.add(Timestamp.from(filter.to()));
        return args;
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
