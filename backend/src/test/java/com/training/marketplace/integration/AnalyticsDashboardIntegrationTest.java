package com.training.marketplace.integration;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.dto.response.ProductPerformanceResponse;
import com.training.marketplace.dto.response.PromotionRecommendationPerformanceResponse;
import com.training.marketplace.repository.AnalyticsDashboardQueryRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeout;

@Testcontainers(disabledWithoutDocker = true)
class AnalyticsDashboardIntegrationTest {

    private static final Instant FROM = Instant.parse("2099-01-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2099-02-01T00:00:00Z");

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("analytics_test")
            .withUsername("test")
            .withPassword("test");

    private static JdbcTemplate jdbcTemplate;
    private static AnalyticsDashboardQueryRepository queryRepository;

    @BeforeAll
    static void setUpDatabase() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .load()
                .migrate();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        queryRepository = new AnalyticsDashboardQueryRepository(jdbcTemplate);
    }

    @AfterEach
    void removeTestEvents() {
        jdbcTemplate.update("delete from analytics_events where session_id = 'analytics-integration-test'");
        jdbcTemplate.update("delete from merchandising_events where session_id = 'analytics-integration-test'");
        jdbcTemplate.update("delete from campaigns where name = 'Analytics Integration Campaign'");
    }

    @Test
    void overview_calculatesKpisWithFiltersAndHandlesNoEvents() {
        insertEvent("PRODUCT_VIEW", null, null);
        insertEvent("ADD_TO_CART", null, null);
        insertEvent("BEGIN_CHECKOUT", null, null);
        insertEvent("ORDER_CREATED", null, 900001L);

        AnalyticsOverviewResponse overview = queryRepository.overview(filter("launch", "mobile"));

        assertThat(overview.productViews()).isEqualTo(1);
        assertThat(overview.addToCarts()).isEqualTo(1);
        assertThat(overview.beginCheckouts()).isEqualTo(1);
        assertThat(overview.orders()).isEqualTo(1);
        assertThat(overview.returningCustomerRate()).isZero();
        assertThat(queryRepository.overview(new AnalyticsDashboardFilter(
                FROM.minus(Duration.ofDays(30)), FROM.minus(Duration.ofDays(1)),
                null, null, null, null, null)).productViews()).isZero();
    }

    @Test
    void productPerformance_appliesAttributionFiltersWithinQueryThreshold() {
        Long productId = jdbcTemplate.queryForObject("select id from products order by id limit 1", Long.class);
        insertEvent("PRODUCT_VIEW", productId, null);
        insertEvent("ORDER_CREATED", productId, 900002L);

        PageResponse<ProductPerformanceResponse> page = assertTimeout(Duration.ofSeconds(5), () ->
                queryRepository.productPerformance(
                        new AnalyticsDashboardFilter(FROM, TO, null, productId,
                                "launch", "HOME_BEST_SELLERS", "mobile"),
                        0, 20, null, "productViews", "desc"));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).productViews()).isEqualTo(1);
        assertThat(page.getContent().get(0).orders()).isEqualTo(1);
    }

    @Test
    void promotionPerformance_includesStorefrontCampaignEvents() {
        Long campaignId = jdbcTemplate.queryForObject("""
                insert into campaigns (name, status, active)
                values ('Analytics Integration Campaign', 'PUBLISHED', true)
                returning id
                """, Long.class);
        insertMerchandisingEvent(campaignId, "IMPRESSION");
        insertMerchandisingEvent(campaignId, "CLICK");

        AnalyticsDashboardFilter filter = new AnalyticsDashboardFilter(
                FROM, TO, null, null, "Analytics Integration Campaign",
                "CAMPAIGN_STRIP", null);
        List<PromotionRecommendationPerformanceResponse> rows =
                queryRepository.promotionRecommendationPerformance(filter);

        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.campaign()).isEqualTo("Analytics Integration Campaign");
            assertThat(row.placement()).isEqualTo("CAMPAIGN_STRIP");
            assertThat(row.impressions()).isEqualTo(1);
            assertThat(row.clicks()).isEqualTo(1);
        });
        assertThat(queryRepository.promotionTrend(filter)).singleElement().satisfies(point -> {
            assertThat(point.impressions()).isEqualTo(1);
            assertThat(point.clicks()).isEqualTo(1);
        });
    }

    private AnalyticsDashboardFilter filter(String campaign, String deviceType) {
        return new AnalyticsDashboardFilter(
                FROM, TO, null, null, campaign, "HOME_BEST_SELLERS", deviceType);
    }

    private void insertEvent(String eventType, Long productId, Long orderId) {
        jdbcTemplate.update("""
                insert into analytics_events (
                    event_id, schema_version, event_type, session_id, product_id, placement,
                    order_id, occurred_at, received_at, properties
                ) values (?, 1, ?, 'analytics-integration-test', ?, 'HOME_BEST_SELLERS',
                          ?, ?::timestamptz, ?::timestamptz, cast(? as jsonb))
                """, UUID.randomUUID(), eventType, productId, orderId,
                FROM.plusSeconds(60).toString(), FROM.plusSeconds(61).toString(),
                "{\"campaign\":\"launch\",\"deviceType\":\"mobile\"}");
    }

    private void insertMerchandisingEvent(Long campaignId, String eventType) {
        jdbcTemplate.update("""
                insert into merchandising_events (
                    event_id, event_type, target_type, target_id, session_id, occurred_at, received_at
                ) values (?, ?, 'CAMPAIGN', ?, 'analytics-integration-test', ?::timestamp, ?::timestamp)
                """, UUID.randomUUID(), eventType, campaignId,
                FROM.plusSeconds(120).toString(), FROM.plusSeconds(121).toString());
    }
}
