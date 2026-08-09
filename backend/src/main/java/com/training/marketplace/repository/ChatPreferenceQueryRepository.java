package com.training.marketplace.repository;

import com.training.marketplace.service.ChatPreferenceProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashSet;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ChatPreferenceQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatPreferenceProfile load(Long userId, String sessionId) {
        if (userId == null && (sessionId == null || sessionId.isBlank())) {
            return ChatPreferenceProfile.empty();
        }
        String sql = """
                SELECT p.category_id,
                       p.brand,
                       SUM(CASE ae.event_type
                               WHEN 'PURCHASE' THEN 8
                               WHEN 'ADD_TO_CART' THEN 5
                               WHEN 'ADD_TO_WISHLIST' THEN 4
                               WHEN 'PRODUCT_VIEW' THEN 2
                               WHEN 'SEARCH' THEN 1
                               ELSE 0
                           END) AS affinity_score
                  FROM analytics_events ae
                  JOIN products p ON p.id = ae.product_id
                 WHERE ae.occurred_at >= ?
                   AND ((CAST(? AS BIGINT) IS NOT NULL AND ae.user_id = CAST(? AS BIGINT))
                        OR (CAST(? AS VARCHAR) IS NOT NULL AND ae.session_id = CAST(? AS VARCHAR)))
                   AND p.active = TRUE
                 GROUP BY p.category_id, p.brand
                HAVING SUM(CASE ae.event_type
                               WHEN 'PURCHASE' THEN 8
                               WHEN 'ADD_TO_CART' THEN 5
                               WHEN 'ADD_TO_WISHLIST' THEN 4
                               WHEN 'PRODUCT_VIEW' THEN 2
                               WHEN 'SEARCH' THEN 1
                               ELSE 0
                           END) > 0
                 ORDER BY affinity_score DESC
                 LIMIT 5
                """;
        Set<Long> categories = new LinkedHashSet<>();
        Set<String> brands = new LinkedHashSet<>();
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> collect(rs, categories, brands),
                Instant.now().minus(90, ChronoUnit.DAYS),
                userId, userId, normalized(sessionId), normalized(sessionId));
        return new ChatPreferenceProfile(categories, brands);
    }

    private void collect(ResultSet rs, Set<Long> categories, Set<String> brands)
            throws java.sql.SQLException {
        long categoryId = rs.getLong("category_id");
        if (!rs.wasNull()) {
            categories.add(categoryId);
        }
        String brand = rs.getString("brand");
        if (brand != null && !brand.isBlank()) {
            brands.add(brand);
        }
    }

    private String normalized(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? null : sessionId.trim();
    }
}
