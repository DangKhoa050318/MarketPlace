package com.training.marketplace.repository;

import com.training.marketplace.service.ChatProductCandidate;
import com.training.marketplace.service.ChatSearchCriteria;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ChatProductQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<ChatProductCandidate> search(ChatSearchCriteria criteria) {
        return query(criteria, List.of());
    }

    public List<ChatProductCandidate> findByProductIds(
            Collection<Long> productIds,
            ChatSearchCriteria criteria) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        return query(criteria, productIds);
    }

    private List<ChatProductCandidate> query(
            ChatSearchCriteria criteria,
            Collection<Long> forcedProductIds) {
        List<Object> params = new ArrayList<>();
        List<String> clauses = new ArrayList<>();
        clauses.add("p.active = TRUE");

        if (!forcedProductIds.isEmpty()) {
            clauses.add("p.id IN (" + placeholders(forcedProductIds.size()) + ")");
            params.addAll(forcedProductIds);
        }
        if (hasText(criteria.query())) {
            for (String token : tokens(criteria.query())) {
                String like = "%" + token.toLowerCase() + "%";
            clauses.add("""
                    (LOWER(p.name) LIKE ?
                     OR LOWER(COALESCE(p.description, '')) LIKE ?
                     OR LOWER(COALESCE(p.brand, '')) LIKE ?
                     OR LOWER(CAST(p.attributes AS text)) LIKE ?
                     OR LOWER(c.name) LIKE ?
                     OR EXISTS (
                         SELECT 1 FROM product_variants search_variant
                          WHERE search_variant.product_id = p.id
                            AND search_variant.active = TRUE
                            AND LOWER(COALESCE(NULLIF(TRIM(CONCAT(search_variant.color, ' ', search_variant.size)), ''),
                                               search_variant.variant_name)) LIKE ?
                     ))
                    """);
                for (int i = 0; i < 6; i++) {
                    params.add(like);
                }
            }
        }
        if (hasText(criteria.category())) {
            clauses.add("LOWER(c.name) LIKE ?");
            params.add("%" + criteria.category().trim().toLowerCase() + "%");
        }
        if (criteria.pageCategoryId() != null) {
            clauses.add("p.category_id = ?");
            params.add(criteria.pageCategoryId());
        }
        if (criteria.minPrice() != null) {
            clauses.add("v.price >= ?");
            params.add(criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            clauses.add("v.price <= ?");
            params.add(criteria.maxPrice());
        }
        if (hasText(criteria.brand())) {
            clauses.add("LOWER(COALESCE(p.brand, '')) LIKE ?");
            params.add("%" + criteria.brand().trim().toLowerCase() + "%");
        }
        appendOfferScope(criteria, clauses, params);

        String sql = """
                SELECT p.id,
                       p.slug,
                       p.name,
                       p.description,
                       p.category_id,
                       c.name AS category_name,
                       p.brand,
                       p.image_url,
                       MIN(v.price) AS min_price,
                       MAX(v.price) AS max_price,
                       COALESCE(SUM(sl.quantity - sl.reserved_quantity), 0) AS available_stock
                  FROM products p
                  JOIN categories c ON c.id = p.category_id
                  JOIN product_variants v ON v.product_id = p.id AND v.active = TRUE
             LEFT JOIN stock_levels sl ON sl.variant_id = v.id
                 WHERE
                """ + String.join(" AND ", clauses) + "\n" + """
              GROUP BY p.id, c.name
                HAVING COALESCE(SUM(sl.quantity - sl.reserved_quantity), 0) > 0
              ORDER BY available_stock DESC, p.created_at DESC, p.id ASC
                 LIMIT ?
                """;
        params.add(criteria.limit());

        return jdbcTemplate.query(sql, (rs, rowNum) -> new ChatProductCandidate(
                rs.getLong("id"),
                rs.getString("slug"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getLong("category_id"),
                rs.getString("category_name"),
                rs.getString("brand"),
                rs.getString("image_url"),
                rs.getBigDecimal("min_price"),
                rs.getBigDecimal("max_price"),
                rs.getLong("available_stock")), params.toArray());
    }

    private void appendOfferScope(
            ChatSearchCriteria criteria,
            List<String> clauses,
            List<Object> params) {
        if (!criteria.offerOnly()) {
            return;
        }
        if (criteria.cartWideOffer()) {
            return;
        }
        List<String> offerClauses = new ArrayList<>();
        if (!criteria.productScopeIds().isEmpty()) {
            offerClauses.add("p.id IN (" + placeholders(criteria.productScopeIds().size()) + ")");
            params.addAll(criteria.productScopeIds());
        }
        if (!criteria.categoryScopeIds().isEmpty()) {
            offerClauses.add("p.category_id IN (" + placeholders(criteria.categoryScopeIds().size()) + ")");
            params.addAll(criteria.categoryScopeIds());
        }
        if (!offerClauses.isEmpty()) {
            clauses.add("(" + String.join(" OR ", offerClauses) + ")");
        } else {
            clauses.add("1 = 0");
        }
    }

    private String placeholders(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(ignored -> "?")
                .collect(Collectors.joining(","));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private List<String> tokens(String query) {
        return java.util.Arrays.stream(query.trim().split("\\s+"))
                .map(String::trim)
                .filter(token -> token.length() >= 2)
                .distinct()
                .limit(8)
                .toList();
    }
}
