package com.training.marketplace.repository;

import com.training.marketplace.service.RecommendationCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SimilarProductRecommendationRepository {

    private static final String COMPUTE_SQL = """
            WITH source AS (
                SELECT p.id,
                       p.category_id,
                       LOWER(BTRIM(p.brand)) AS normalized_brand,
                       MIN(v.price) AS min_price,
                       MAX(v.price) AS max_price
                  FROM products p
                  JOIN product_variants v
                    ON v.product_id = p.id
                   AND v.active = TRUE
                 WHERE p.id = :sourceProductId
                   AND p.active = TRUE
                 GROUP BY p.id, p.category_id, LOWER(BTRIM(p.brand))
            ),
            category_candidates AS (
                SELECT p.id
                  FROM products p
                 CROSS JOIN source s
                 WHERE p.active = TRUE
                   AND p.id <> s.id
                   AND s.category_id IS NOT NULL
                   AND p.category_id = s.category_id
                 ORDER BY p.id
                 LIMIT :candidatePoolSize
            ),
            brand_candidates AS (
                SELECT p.id
                  FROM products p
                 CROSS JOIN source s
                 WHERE p.active = TRUE
                   AND p.id <> s.id
                   AND s.normalized_brand IS NOT NULL
                   AND s.normalized_brand <> ''
                   AND LOWER(BTRIM(p.brand)) = s.normalized_brand
                 ORDER BY p.id
                 LIMIT :candidatePoolSize
            ),
            attribute_candidates AS (
                SELECT candidate_term.product_id AS id
                  FROM product_attribute_terms source_term
                  JOIN product_attribute_terms candidate_term
                    ON candidate_term.term = source_term.term
                   AND candidate_term.product_id <> source_term.product_id
                  JOIN products p
                    ON p.id = candidate_term.product_id
                   AND p.active = TRUE
                 WHERE source_term.product_id = :sourceProductId
                 GROUP BY candidate_term.product_id
                 ORDER BY COUNT(*) DESC, candidate_term.product_id
                 LIMIT :candidatePoolSize
            ),
            candidate_ids AS (
                SELECT id FROM category_candidates
                UNION
                SELECT id FROM brand_candidates
                UNION
                SELECT id FROM attribute_candidates
            ),
            candidate_base AS (
                SELECT p.id,
                       p.category_id,
                       LOWER(BTRIM(p.brand)) AS normalized_brand,
                       MIN(v.price) AS min_price,
                       MAX(v.price) AS max_price
                  FROM candidate_ids candidate
                  JOIN products p ON p.id = candidate.id AND p.active = TRUE
                  JOIN product_variants v
                    ON v.product_id = p.id
                   AND v.active = TRUE
                 GROUP BY p.id, p.category_id, LOWER(BTRIM(p.brand))
            ),
            similarity_metrics AS (
                SELECT candidate.id,
                       candidate.category_id = source.category_id
                           AND source.category_id IS NOT NULL AS same_category,
                       candidate.normalized_brand = source.normalized_brand
                           AND source.normalized_brand IS NOT NULL
                           AND source.normalized_brand <> '' AS same_brand,
                       COALESCE(attribute_stats.similarity, 0.0) AS attribute_similarity,
                       CASE
                           WHEN source.min_price <= candidate.max_price
                            AND candidate.min_price <= source.max_price THEN 1.0
                           ELSE GREATEST(
                               0.0,
                               1.0 - (
                                   CASE
                                       WHEN source.max_price < candidate.min_price
                                           THEN candidate.min_price - source.max_price
                                       ELSE source.min_price - candidate.max_price
                                   END
                               )::double precision / NULLIF(GREATEST(
                                   (source.min_price + source.max_price) / 2,
                                   (candidate.min_price + candidate.max_price) / 2
                               )::double precision, 0.0)
                           )
                       END AS price_similarity
                  FROM candidate_base candidate
                 CROSS JOIN source
                  LEFT JOIN LATERAL (
                      SELECT COUNT(overlap.term)::double precision
                                 / NULLIF(
                                     source_count.term_count
                                     + candidate_count.term_count
                                     - COUNT(overlap.term),
                                     0
                                 ) AS similarity
                        FROM (SELECT COUNT(*) AS term_count
                                FROM product_attribute_terms
                               WHERE product_id = source.id) source_count
                       CROSS JOIN (SELECT COUNT(*) AS term_count
                                     FROM product_attribute_terms
                                    WHERE product_id = candidate.id) candidate_count
                        LEFT JOIN (
                            SELECT source_term.term
                              FROM product_attribute_terms source_term
                              JOIN product_attribute_terms candidate_term
                                ON candidate_term.term = source_term.term
                               AND candidate_term.product_id = candidate.id
                             WHERE source_term.product_id = source.id
                        ) overlap ON TRUE
                       GROUP BY source_count.term_count, candidate_count.term_count
                  ) attribute_stats ON TRUE
            ),
            scored AS (
                SELECT id,
                       (CASE WHEN same_category THEN 0.35 ELSE 0.0 END)
                       + (CASE WHEN same_brand THEN 0.25 ELSE 0.0 END)
                       + attribute_similarity * 0.25
                       + price_similarity * 0.15 AS score,
                       CONCAT_WS(',',
                           CASE WHEN same_category THEN 'same_category' END,
                           CASE WHEN same_brand THEN 'same_brand' END,
                           CASE WHEN attribute_similarity > 0 THEN 'matching_attributes' END,
                           CASE WHEN price_similarity >= 0.50 THEN 'similar_price' END
                       ) AS reason
                  FROM similarity_metrics
            )
            SELECT id, score, reason
              FROM scored
             ORDER BY score DESC, id
             LIMIT :precomputeSize
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public void acquireSourceLock(Long sourceProductId) {
        jdbcTemplate.query(
                "SELECT pg_advisory_xact_lock(:sourceProductId)",
                new MapSqlParameterSource("sourceProductId", sourceProductId),
                resultSet -> null);
    }

    public boolean isComputed(Long sourceProductId) {
        Boolean computed = jdbcTemplate.queryForObject(
                """
                        SELECT EXISTS (
                            SELECT 1 FROM product_similarity_status
                             WHERE source_product_id = :sourceProductId
                        )
                        """,
                new MapSqlParameterSource("sourceProductId", sourceProductId),
                Boolean.class);
        return Boolean.TRUE.equals(computed);
    }

    public List<RecommendationCandidate> findTop(Long sourceProductId, int limit) {
        return jdbcTemplate.query(
                """
                        SELECT candidate_product_id, score, reason
                          FROM product_similarities
                         WHERE source_product_id = :sourceProductId
                         ORDER BY rank
                         LIMIT :limit
                        """,
                new MapSqlParameterSource()
                        .addValue("sourceProductId", sourceProductId)
                        .addValue("limit", limit),
                (rs, rowNum) -> new RecommendationCandidate(
                        rs.getLong("candidate_product_id"),
                        rs.getDouble("score"),
                        rs.getString("reason")));
    }

    public List<RecommendationCandidate> computeTop(
            Long sourceProductId,
            int candidatePoolSize,
            int precomputeSize) {
        return jdbcTemplate.query(
                COMPUTE_SQL,
                new MapSqlParameterSource()
                        .addValue("sourceProductId", sourceProductId)
                        .addValue("candidatePoolSize", candidatePoolSize)
                        .addValue("precomputeSize", precomputeSize),
                (rs, rowNum) -> new RecommendationCandidate(
                        rs.getLong("id"),
                        rs.getDouble("score"),
                        rs.getString("reason")));
    }

    public void replace(Long sourceProductId, List<RecommendationCandidate> candidates) {
        jdbcTemplate.update(
                "DELETE FROM product_similarities WHERE source_product_id = :sourceProductId",
                new MapSqlParameterSource("sourceProductId", sourceProductId));

        if (!candidates.isEmpty()) {
            MapSqlParameterSource[] batch = new MapSqlParameterSource[candidates.size()];
            for (int index = 0; index < candidates.size(); index++) {
                RecommendationCandidate candidate = candidates.get(index);
                batch[index] = new MapSqlParameterSource()
                        .addValue("sourceProductId", sourceProductId)
                        .addValue("candidateProductId", candidate.productId())
                        .addValue("score", candidate.score())
                        .addValue("reason", candidate.reason())
                        .addValue("rank", index + 1);
            }
            jdbcTemplate.batchUpdate(
                    """
                            INSERT INTO product_similarities (
                                source_product_id, candidate_product_id, score, reason, rank
                            ) VALUES (
                                :sourceProductId, :candidateProductId, :score, :reason, :rank
                            )
                            """,
                    batch);
        }

        jdbcTemplate.update(
                """
                        INSERT INTO product_similarity_status (source_product_id, computed_at)
                        VALUES (:sourceProductId, NOW())
                        ON CONFLICT (source_product_id)
                        DO UPDATE SET computed_at = EXCLUDED.computed_at
                        """,
                new MapSqlParameterSource("sourceProductId", sourceProductId));
    }

    public void invalidateAffected(Long productId) {
        jdbcTemplate.update(
                """
                        WITH changed AS (
                            SELECT id, category_id, LOWER(BTRIM(brand)) AS normalized_brand
                              FROM products
                             WHERE id = :productId
                        ),
                        affected_sources AS (
                            SELECT :productId AS source_product_id
                            UNION
                            SELECT source_product_id
                              FROM product_similarities
                             WHERE candidate_product_id = :productId
                            UNION
                            SELECT candidate.id
                              FROM products candidate
                             CROSS JOIN changed
                             WHERE candidate.active = TRUE
                               AND candidate.id <> changed.id
                               AND (
                                   (changed.category_id IS NOT NULL
                                       AND candidate.category_id = changed.category_id)
                                   OR (changed.normalized_brand IS NOT NULL
                                       AND changed.normalized_brand <> ''
                                       AND LOWER(BTRIM(candidate.brand)) = changed.normalized_brand)
                                   OR EXISTS (
                                       SELECT 1
                                         FROM product_attribute_terms changed_term
                                         JOIN product_attribute_terms candidate_term
                                           ON candidate_term.term = changed_term.term
                                          AND candidate_term.product_id = candidate.id
                                        WHERE changed_term.product_id = changed.id
                                   )
                               )
                        )
                        DELETE FROM product_similarity_status status
                         WHERE status.source_product_id IN (
                             SELECT source_product_id FROM affected_sources
                         )
                        """,
                new MapSqlParameterSource("productId", productId));
    }
}
