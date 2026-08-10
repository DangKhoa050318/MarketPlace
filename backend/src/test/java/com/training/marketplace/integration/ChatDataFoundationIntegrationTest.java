package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.repository.ChatCampaignOfferRepository;
import com.training.marketplace.repository.ChatProductQueryRepository;
import com.training.marketplace.service.ChatSearchCriteria;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ChatDataFoundationIntegrationTest extends BaseIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ChatCampaignOfferRepository campaignOfferRepository;
    @Autowired private ChatProductQueryRepository productQueryRepository;

    @Test
    void returnsOnlyInStockProductsFromAnEffectiveScopedCampaign() {
        LocalDateTime now = LocalDateTime.now();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Long categoryId = jdbcTemplate.queryForObject("""
                INSERT INTO categories (name, code, slug)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, "Chat test category " + suffix,
                "CHAT-" + suffix.toUpperCase(), "chat-test-category-" + suffix);
        Long productId = jdbcTemplate.queryForObject("""
                INSERT INTO products (slug, name, description, category_id, unit, active)
                VALUES (?, ?, 'Search fixture', ?, 'PCS', TRUE)
                RETURNING id
                """, Long.class, "chat-search-laptop-" + suffix,
                "Chat Search Laptop " + suffix, categoryId);
        Long variantId = jdbcTemplate.queryForObject("""
                INSERT INTO product_variants (product_id, sku, variant_name, price, active)
                VALUES (?, ?, 'Default', 2499.00, TRUE)
                RETURNING id
                """, Long.class, productId, "CHAT-SKU-" + suffix.toUpperCase());
        Long warehouseId = jdbcTemplate.queryForObject("""
                INSERT INTO warehouses (name, code, active)
                VALUES (?, ?, TRUE)
                RETURNING id
                """, Long.class, "Chat warehouse " + suffix,
                "CHAT-WH-" + suffix.toUpperCase());
        jdbcTemplate.update("""
                INSERT INTO stock_levels (variant_id, warehouse_id, quantity, reserved_quantity, version)
                VALUES (?, ?, 5, 0, 0)
                """, variantId, warehouseId);

        Long promotionId = jdbcTemplate.queryForObject("""
                INSERT INTO promotion_codes (
                    code, discount_type, discount_value, max_discount, min_order_amount,
                    scope_type, usage_limit, per_user_limit, used_count, starts_at, expires_at, active)
                VALUES (?, 'PERCENT', 10, 100, 0,
                        'PRODUCT', 10, 1, 0, ?, ?, TRUE)
                RETURNING id
                """, Long.class, "CHAT_TEST_" + suffix.toUpperCase(),
                now.minusDays(1), now.plusDays(2));
        Long campaignId = jdbcTemplate.queryForObject("""
                INSERT INTO campaigns (
                    name, description, status, starts_at, ends_at, promotion_code_id, active)
                VALUES ('Chat test campaign', 'Integration fixture', 'PUBLISHED', ?, ?, ?, TRUE)
                RETURNING id
                """, Long.class, now.minusHours(1), now.plusDays(1), promotionId);
        jdbcTemplate.update("""
                INSERT INTO promotion_code_scopes (promotion_code_id, ref_type, ref_id)
                VALUES (?, 'PRODUCT', ?)
                """, promotionId, productId);

        var offers = campaignOfferRepository.findEffective(null, now);
        assertThat(offers).anySatisfy(offer -> {
            assertThat(offer.campaignId()).isEqualTo(campaignId);
            assertThat(offer.appliesTo(productId, categoryId)).isTrue();
        });

        var products = productQueryRepository.search(new ChatSearchCriteria(
                "Chat Search Laptop", null, null, BigDecimal.ZERO, new BigDecimal("3000"), null,
                Set.of(productId), Set.of(), true, false, 6));
        assertThat(products).extracting(product -> product.productId()).contains(productId);
        assertThat(products.get(0).availableStock()).isPositive();

        jdbcTemplate.update("UPDATE promotion_codes SET expires_at = ? WHERE id = ?",
                now.minusMinutes(1), promotionId);
        assertThat(campaignOfferRepository.findEffective(null, now)).noneMatch(offer ->
                offer.campaignId().equals(campaignId));
    }
}
