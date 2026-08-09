package com.training.marketplace.repository;

import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.ScopeRefType;
import com.training.marketplace.service.ChatCampaignOffer;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository
@RequiredArgsConstructor
public class ChatCampaignOfferRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<ChatCampaignOffer> findEffective(Long userId, LocalDateTime now) {
        String sql = """
                SELECT c.id AS campaign_id,
                       c.name AS campaign_name,
                       c.description AS campaign_description,
                       p.id AS promotion_id,
                       p.code,
                       p.discount_type,
                       p.discount_value,
                       p.max_discount,
                       p.min_order_amount,
                       p.scope_type,
                       p.expires_at,
                       pcs.ref_type,
                       pcs.ref_id
                  FROM campaigns c
                  JOIN promotion_codes p ON p.id = c.promotion_code_id
             LEFT JOIN promotion_code_scopes pcs ON pcs.promotion_code_id = p.id
             LEFT JOIN (
                       SELECT promotion_code_id, COUNT(*) AS redemption_count
                         FROM promotion_redemptions
                        WHERE user_id = CAST(? AS BIGINT)
                        GROUP BY promotion_code_id
                       ) user_usage ON user_usage.promotion_code_id = p.id
                 WHERE c.status = 'PUBLISHED'
                   AND c.active = TRUE
                   AND (c.starts_at IS NULL OR c.starts_at <= ?)
                   AND (c.ends_at IS NULL OR c.ends_at >= ?)
                   AND p.active = TRUE
                   AND (p.starts_at IS NULL OR p.starts_at <= ?)
                   AND (p.expires_at IS NULL OR p.expires_at >= ?)
                   AND (p.usage_limit IS NULL OR p.used_count < p.usage_limit)
                   AND (p.per_user_limit IS NULL OR CAST(? AS BIGINT) IS NULL
                        OR COALESCE(user_usage.redemption_count, 0) < p.per_user_limit)
              ORDER BY c.starts_at DESC NULLS LAST, c.id, pcs.id
                """;

        Map<Long, OfferAccumulator> offers = new LinkedHashMap<>();
        jdbcTemplate.query(sql, (RowCallbackHandler) rs -> accumulate(offers, rs),
                userId, now, now, now, now, userId);
        return offers.values().stream().map(OfferAccumulator::toOffer).toList();
    }

    private void accumulate(Map<Long, OfferAccumulator> offers, ResultSet rs) throws SQLException {
        Long campaignId = rs.getLong("campaign_id");
        OfferAccumulator offer = offers.computeIfAbsent(campaignId, ignored -> fromRow(rs));
        String refType = rs.getString("ref_type");
        Long refId = nullableLong(rs, "ref_id");
        if (refType != null && refId != null) {
            offer.scopes.computeIfAbsent(ScopeRefType.valueOf(refType), ignored -> new LinkedHashSet<>())
                    .add(refId);
        }
    }

    private OfferAccumulator fromRow(ResultSet rs) {
        try {
            return new OfferAccumulator(
                    rs.getLong("campaign_id"),
                    rs.getString("campaign_name"),
                    rs.getString("campaign_description"),
                    rs.getLong("promotion_id"),
                    rs.getString("code"),
                    DiscountType.valueOf(rs.getString("discount_type")),
                    rs.getBigDecimal("discount_value"),
                    rs.getBigDecimal("max_discount"),
                    rs.getBigDecimal("min_order_amount"),
                    PromotionScopeType.valueOf(rs.getString("scope_type")),
                    rs.getTimestamp("expires_at") == null
                            ? null : rs.getTimestamp("expires_at").toLocalDateTime());
        } catch (SQLException exception) {
            throw new IllegalStateException("Unable to map campaign offer", exception);
        }
    }

    private Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private static final class OfferAccumulator {
        private final Long campaignId;
        private final String campaignName;
        private final String campaignDescription;
        private final Long promotionCodeId;
        private final String code;
        private final DiscountType discountType;
        private final BigDecimal discountValue;
        private final BigDecimal maxDiscount;
        private final BigDecimal minOrderAmount;
        private final PromotionScopeType scopeType;
        private final LocalDateTime expiresAt;
        private final Map<ScopeRefType, Set<Long>> scopes = new EnumMap<>(ScopeRefType.class);

        private OfferAccumulator(
                Long campaignId,
                String campaignName,
                String campaignDescription,
                Long promotionCodeId,
                String code,
                DiscountType discountType,
                BigDecimal discountValue,
                BigDecimal maxDiscount,
                BigDecimal minOrderAmount,
                PromotionScopeType scopeType,
                LocalDateTime expiresAt) {
            this.campaignId = campaignId;
            this.campaignName = campaignName;
            this.campaignDescription = campaignDescription;
            this.promotionCodeId = promotionCodeId;
            this.code = code;
            this.discountType = discountType;
            this.discountValue = discountValue;
            this.maxDiscount = maxDiscount;
            this.minOrderAmount = minOrderAmount;
            this.scopeType = scopeType;
            this.expiresAt = expiresAt;
        }

        private ChatCampaignOffer toOffer() {
            Map<ScopeRefType, Set<Long>> immutableScopes = new EnumMap<>(ScopeRefType.class);
            scopes.forEach((type, ids) -> immutableScopes.put(type, Set.copyOf(ids)));
            return new ChatCampaignOffer(
                    campaignId, campaignName, campaignDescription, promotionCodeId, code,
                    discountType, discountValue, maxDiscount, minOrderAmount, scopeType,
                    expiresAt, immutableScopes);
        }
    }
}
