package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.repository.SimilarProductRecommendationRepository;
import com.training.marketplace.service.RecommendationCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SimilarProductRecommendationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SimilarProductRecommendationRepository repository;

    @Test
    void computesAndPersistsRankedTopKWithoutLoadingCatalogEntities() {
        Long categoryId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO categories (name, code, slug)
                        VALUES ('Similarity Test', 'SIMTEST', 'similarity-test')
                        RETURNING id
                        """,
                Long.class);
        Long sourceId = insertProduct(
                categoryId, "sim-source", "Acme",
                "{\"type\":\"laptop\",\"ram\":\"16 gb\"}", "1000.00");
        Long strongestId = insertProduct(
                categoryId, "sim-strongest", "acme",
                "{\"type\":\"laptop\",\"ram\":\"16 gb\"}", "1050.00");
        Long categoryOnlyId = insertProduct(
                categoryId, "sim-category", "Other",
                "{\"type\":\"tablet\"}", "1000.00");
        insertProduct(
                null, "sim-unrelated", "Different",
                "{\"type\":\"camera\"}", "1000.00");

        List<RecommendationCandidate> computed = repository.computeTop(sourceId, 100, 10);

        assertThat(computed)
                .extracting(RecommendationCandidate::productId)
                .startsWith(strongestId, categoryOnlyId);
        assertThat(computed.get(0).reason())
                .contains("same_category", "same_brand", "matching_attributes", "similar_price");

        repository.replace(sourceId, computed);

        assertThat(repository.isComputed(sourceId)).isTrue();
        assertThat(repository.findTop(sourceId, 1))
                .extracting(RecommendationCandidate::productId)
                .containsExactly(strongestId);
    }

    private Long insertProduct(
            Long categoryId,
            String slug,
            String brand,
            String attributes,
            String price) {
        Long productId = jdbcTemplate.queryForObject(
                """
                        INSERT INTO products (slug, name, category_id, brand, attributes, active)
                        VALUES (?, ?, ?, ?, CAST(? AS jsonb), TRUE)
                        RETURNING id
                        """,
                Long.class,
                slug,
                slug,
                categoryId,
                brand,
                attributes);
        jdbcTemplate.update(
                """
                        INSERT INTO product_variants (
                            product_id, sku, variant_name, price, active
                        ) VALUES (?, ?, 'Default', CAST(? AS numeric), TRUE)
                        """,
                productId,
                slug.toUpperCase(),
                price);
        return productId;
    }
}
