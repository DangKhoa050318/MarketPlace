package com.training.marketplace.repository;

import com.training.marketplace.dto.request.ProductCatalogFilter;
import com.training.marketplace.dto.response.StorefrontProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
                       COUNT(DISTINCT v.id) AS variant_count, p.created_at
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
                        rs.getTimestamp("created_at").toLocalDateTime()),
                dataParams.toArray());

        String countSql = "SELECT COUNT(*) FROM (SELECT p.id FROM products p " +
                "JOIN categories c ON c.id = p.category_id " +
                "JOIN product_variants v ON v.product_id = p.id AND v.active = TRUE " +
                "LEFT JOIN stock_levels sl ON sl.variant_id = v.id " +
                where + " GROUP BY p.id" + having + ") catalog";
        Long total = jdbcTemplate.queryForObject(countSql, Long.class, params.toArray());
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private String buildWhere(ProductCatalogFilter filter, List<Object> params) {
        var clauses = new ArrayList<String>();
        clauses.add("p.active = TRUE");
        if (filter.query() != null && !filter.query().isBlank()) {
            clauses.add("(LOWER(p.name) LIKE ? OR LOWER(p.description) LIKE ? OR LOWER(p.slug) LIKE ?)");
            String query = "%" + filter.query().trim().toLowerCase() + "%";
            params.add(query);
            params.add(query);
            params.add(query);
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
