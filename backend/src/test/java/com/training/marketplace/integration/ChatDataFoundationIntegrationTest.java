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

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class ChatDataFoundationIntegrationTest extends BaseIntegrationTest {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ChatCampaignOfferRepository campaignOfferRepository;
    @Autowired private ChatProductQueryRepository productQueryRepository;

    @Test
    void returnsOnlyInStockProductsFromAnEffectiveScopedCampaign() {
        LocalDateTime now = LocalDateTime.now();
        Long promotionId = jdbcTemplate.queryForObject("""
                INSERT INTO promotion_codes (
                    code, discount_type, discount_value, max_discount, min_order_amount,
                    scope_type, usage_limit, per_user_limit, used_count, starts_at, expires_at, active)
                VALUES ('CHAT_TEST_10', 'PERCENT', 10, 100, 0,
                        'PRODUCT', 10, 1, 0, ?, ?, TRUE)
                RETURNING id
                """, Long.class, now.minusDays(1), now.plusDays(2));
        Long campaignId = jdbcTemplate.queryForObject("""
                INSERT INTO campaigns (
                    name, description, status, starts_at, ends_at, promotion_code_id, active)
                VALUES ('Chat test campaign', 'Integration fixture', 'PUBLISHED', ?, ?, ?, TRUE)
                RETURNING id
                """, Long.class, now.minusHours(1), now.plusDays(1), promotionId);
        jdbcTemplate.update("""
                INSERT INTO promotion_code_scopes (promotion_code_id, ref_type, ref_id)
                VALUES (?, 'PRODUCT', 1)
                """, promotionId);

        var offers = campaignOfferRepository.findEffective(null, now);
        assertThat(offers).anySatisfy(offer -> {
            assertThat(offer.campaignId()).isEqualTo(campaignId);
            assertThat(offer.appliesTo(1L, 2L)).isTrue();
        });

        var products = productQueryRepository.search(new ChatSearchCriteria(
                "MacBook", null, null, BigDecimal.ZERO, new BigDecimal("3000"), null,
                Set.of(1L), Set.of(), true, false, 6));
        assertThat(products).extracting(product -> product.productId()).contains(1L);
        assertThat(products.get(0).availableStock()).isPositive();

        jdbcTemplate.update("UPDATE promotion_codes SET expires_at = ? WHERE id = ?",
                now.minusMinutes(1), promotionId);
        assertThat(campaignOfferRepository.findEffective(null, now)).noneMatch(offer ->
                offer.campaignId().equals(campaignId));
    }
}
