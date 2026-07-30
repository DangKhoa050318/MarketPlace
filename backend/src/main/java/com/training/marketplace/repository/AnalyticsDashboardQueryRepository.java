package com.training.marketplace.repository;

import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
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