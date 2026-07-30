package com.training.marketplace.repository;

import com.training.marketplace.dto.request.ProductCatalogFilter;
import com.training.marketplace.dto.response.StorefrontProductResponse;
import com.training.marketplace.dto.response.SuggestResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class StorefrontCatalogRepository {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "createdAt", "p.created_at",
            "name", "p.name",
            "price", "min_price",
            "stock", "available_stock"
    );

    private final JdbcTemplate jdbcTemplate;

    public Page<StorefrontProductResponse> browse(ProductCatalogFilter filter, Pageable pageable) {
        var params = new ArrayList<Object>();
        String where = buildWhere(filter, params);
        var order = pageable.getSort().stream().findFirst();
        String direction = order.map(value -> value.isAscending() ? "ASC" : "DESC").orElse("DESC");
        String sortColumn = SORT_COLUMNS.getOrDefault(
                order.map(org.springframework.data.domain.Sort.Order::getProperty).orElse("createdAt"),
                "p.created_at");
        String having = Boolean.TRUE.equals(filter.inStock())
                ? " HAVING COALESCE(SUM(sl.quantity - sl.reserved_quantity), 0) > 0" : "";

        String select = """
                SELECT p.id, p.slug, p.name, p.description, p.category_id,
                       c.name AS category_name, p.unit, p.image_url,
                       MIN(v.price) AS min_price, MAX(v.price) AS max_price,
                       COALESCE(SUM(sl.quantity - sl.reserved_quantity), 0) AS available_stock,
                       COUNT(DISTINCT v.id) AS variant_count, p.created_at,
                       STRING_AGG(DISTINCT COALESCE(NULLIF(TRIM(CONCAT(v.color, ' ', v.size)), ''), v.variant_name), ', ') AS variant_names
                  FROM products p
                  JOIN categories c ON c.id = p.category_id
                  JOIN product_variants v ON v.product_id = p.id AND v.active = TRUE
             LEFT JOIN stock_levels sl ON sl.variant_id = v.id
                """ + where + " GROUP BY p.id, c.name" + having +
                " ORDER BY " + sortColumn + " " + direction + ", p.id ASC LIMIT ? OFFSET ?";
        var dataParams = new ArrayList<>(params);
        dataParams.add(pageable.getPageSize());
        dataParams.add(pageable.getOffset());
        List<StorefrontProductResponse> content = jdbcTemplate.query(select,
                (rs, rowNum) -> new StorefrontProductResponse(
                        rs.getLong("id"), rs.getString("slug"), rs.getString("name"),
                        rs.getString("description"), rs.getLong("category_id"),
                        rs.getString("category_name"), rs.getString("unit"), rs.getString("image_url"),
                        rs.getBigDecimal("min_price"), rs.getBigDecimal("max_price"),
                        rs.getLong("available_stock"), rs.getLong("variant_count"),
                        rs.getTimestamp("created_at").toLocalDateTime(),
                        rs.getString("variant_names"),
                        List.of()),
                dataParams.toArray());

        if (!content.isEmpty()) {
            List<Long> productIds = content.stream().map(StorefrontProductResponse::id).toList();
            String inSql = productIds.stream().map(id -> "?").collect(Collectors.joining(","));
            String variantSelect = String.format("""
                SELECT id, product_id,
                       COALESCE(NULLIF(TRIM(CONCAT(color, ' ', size)), ''), variant_name) AS variant_name,
                       price, image_url
                  FROM product_variants
                 WHERE product_id IN (%s) AND active = TRUE
                 ORDER BY id ASC
                """, inSql);

            Map<Long, List<StorefrontProductResponse.StorefrontVariantItem>> variantMap = new java.util.HashMap<>();
            jdbcTemplate.query(variantSelect, rs -> {
                Long productId = rs.getLong("product_id");
                variantMap.computeIfAbsent(productId, k -> new ArrayList<>()).add(
                        new StorefrontProductResponse.StorefrontVariantItem(
                                rs.getLong("id"),
                                rs.getString("variant_name"),
                                rs.getBigDecimal("price"),
                                rs.getString("image_url")
                        )
                );
            }, productIds.toArray());

            content = content.stream()
                    .map(p -> p.withVariants(variantMap.getOrDefault(p.id(), List.of())))
                    .toList();
        }

        String countSql = "SELECT COUNT(*) FROM (SELECT p.id FROM products p " +
                "JOIN categories c ON c.id = p.category_id " +
                "JOIN product_variants v ON v.product_id = p.id AND v.active = TRUE " +
                "LEFT JOIN stock_levels sl ON sl.variant_id = v.id " +
                where + " GROUP BY p.id" + having + ") catalog";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    /** Returns up to {@code limit} product suggestions with id, name, image and price. */
    public List<SuggestResult> suggest(String query, int limit) {
        String like = "%" + query.trim().toLowerCase() + "%";
        String prefix = query.trim().toLowerCase() + "%";
        String sql = """
                SELECT p.id, p.name,
                       COALESCE((SELECT v.image_url FROM product_variants v
                                  WHERE v.product_id = p.id AND v.active = TRUE
                                  ORDER BY v.price ASC LIMIT 1), '') AS image_url,
                       (SELECT MIN(v2.price) FROM product_variants v2
                         WHERE v2.product_id = p.id AND v2.active = TRUE) AS min_price
                  FROM products p
                 WHERE p.active = TRUE
                   AND (LOWER(p.name) LIKE LOWER(?)
                    OR LOWER(p.slug) LIKE LOWER(?)
                    OR EXISTS (SELECT 1 FROM product_variants v3
                                WHERE v3.product_id = p.id AND v3.active = TRUE
                                  AND LOWER(COALESCE(NULLIF(TRIM(CONCAT(v3.color, ' ', v3.size)), ''), v3.variant_name)) LIKE LOWER(?)))
                 ORDER BY
                   CASE WHEN LOWER(p.name) = LOWER(?) THEN 0
                        WHEN LOWER(p.name) LIKE LOWER(?) THEN 1
                        ELSE 2
                   END,
                   p.name
                 LIMIT ?
                """;
        return jdbcTemplate.query(sql,
                (rs, rn) -> new SuggestResult(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("image_url"),
                        rs.getBigDecimal("min_price")
                ),
                like, like, like, query.trim().toLowerCase(), prefix, limit);
    }

    private String buildWhere(ProductCatalogFilter filter, List<Object> params) {
        var clauses = new ArrayList<String>();
        clauses.add("p.active = TRUE");
        if (filter.query() != null && !filter.query().isBlank()) {
            clauses.add("""
                    (LOWER(p.name) LIKE LOWER(?)
                     OR LOWER(p.description) LIKE LOWER(?)
                     OR LOWER(p.slug) LIKE LOWER(?)
                     OR EXISTS (SELECT 1 FROM product_variants v2
                                 WHERE v2.product_id = p.id AND v2.active = TRUE
                                   AND LOWER(COALESCE(NULLIF(TRIM(CONCAT(v2.color, ' ', v2.size)), ''), v2.variant_name)) LIKE LOWER(?)))
                    """);
            String like = "%" + filter.query().trim().toLowerCase() + "%";
            params.add(like);
            params.add(like);
            params.add(like);
            params.add(like);
        }
        if (filter.categoryId() != null) {
            clauses.add("p.category_id = ?");
            params.add(filter.categoryId());
        }
        if (filter.minPrice() != null) {
            clauses.add("v.price >= ?");
            params.add(filter.minPrice());
        }
        if (filter.maxPrice() != null) {
            clauses.add("v.price <= ?");
            params.add(filter.maxPrice());
        }
        return " WHERE " + String.join(" AND ", clauses);
    }
}
