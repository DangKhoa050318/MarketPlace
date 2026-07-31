package com.training.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AnalyticsRetentionRequest;
import com.training.marketplace.dto.response.AnalyticsOverviewResponse;
import com.training.marketplace.dto.response.AnalyticsRetentionResponse;
import com.training.marketplace.dto.response.FunnelStepResponse;
import com.training.marketplace.dto.response.FunnelSummaryResponse;
import com.training.marketplace.dto.response.ProductPerformanceResponse;
import com.training.marketplace.dto.response.PromotionRecommendationPerformanceResponse;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.security.SecurityConfig;
import com.training.marketplace.service.AnalyticsDashboardService;
import com.training.marketplace.service.AnalyticsEventService;
import com.training.marketplace.service.FunnelAnalyticsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAnalyticsController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AdminAnalyticsControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private FunnelAnalyticsService funnelAnalyticsService;
    @MockBean private AnalyticsEventService analyticsEventService;
    @MockBean private AnalyticsDashboardService analyticsDashboardService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(), any(), any());
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(rateLimitingFilter).doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void overview_managerCanReadKpis() throws Exception {
        when(analyticsDashboardService.overview(any()))
                .thenReturn(new AnalyticsOverviewResponse(
                        100,
                        25,
                        10,
                        5,
                        new BigDecimal("0.2500"),
                        new BigDecimal("0.4000"),
                        new BigDecimal("0.0500"),
                        new BigDecimal("0.2000"),
                        Instant.parse("2026-07-30T00:00:00Z")));

        mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z")
                        .param("campaign", "summer")
                        .param("placement", "HOME_BEST_SELLERS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productViews").value(100))
                .andExpect(jsonPath("$.data.addToCartRate").value(0.2500))
                .andExpect(jsonPath("$.data.lastUpdatedAt").value("2026-07-30T00:00:00Z"));

        verify(analyticsDashboardService).overview(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void productPerformance_managerCanReadPagedMetrics() throws Exception {
        when(analyticsDashboardService.productPerformance(any(), any(Integer.class), any(Integer.class)))
                .thenReturn(new PageResponse<>(List.of(new ProductPerformanceResponse(
                        7L,
                        "Mechanical Keyboard",
                        3L,
                        120,
                        18,
                        30,
                        9,
                        new BigDecimal("4.50"),
                        6,
                        Instant.parse("2026-07-30T00:00:00Z"))), 0, 20, 1, 1, true));

        mockMvc.perform(get("/api/v1/admin/analytics/products/performance")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z")
                        .param("categoryId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].productId").value(7))
                .andExpect(jsonPath("$.data.content[0].wishlists").value(18))
                .andExpect(jsonPath("$.data.content[0].averageRating").value(4.50))
                .andExpect(jsonPath("$.data.content[0].questionCount").value(6))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(analyticsDashboardService).productPerformance(any(), any(Integer.class), any(Integer.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void promotionRecommendationPerformance_managerCanReadAttribution() throws Exception {
        when(analyticsDashboardService.promotionRecommendationPerformance(any()))
                .thenReturn(List.of(new PromotionRecommendationPerformanceResponse(
                        "summer",
                        "HOME_BEST_SELLERS",
                        "BEST_SELLER",
                        200,
                        50,
                        new BigDecimal("0.2500"),
                        20,
                        8,
                        Instant.parse("2026-07-31T00:00:00Z"))));

        mockMvc.perform(get("/api/v1/admin/analytics/promotion-recommendation/performance")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z")
                        .param("campaign", "summer")
                        .param("placement", "HOME_BEST_SELLERS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].campaign").value("summer"))
                .andExpect(jsonPath("$.data[0].impressions").value(200))
                .andExpect(jsonPath("$.data[0].clicks").value(50))
                .andExpect(jsonPath("$.data[0].clickThroughRate").value(0.2500))
                .andExpect(jsonPath("$.data[0].attributedOrders").value(8))
                .andExpect(jsonPath("$.data[0].lastUpdatedAt").value("2026-07-31T00:00:00Z"));

        verify(analyticsDashboardService).promotionRecommendationPerformance(any());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void funnel_managerCanReadSummary() throws Exception {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-01T00:00:00Z");
        when(funnelAnalyticsService.summarize(
                from, to, null, null, null, "HOME_BEST_SELLERS", null))
                .thenReturn(new FunnelSummaryResponse(
                        from,
                        to,
                        null,
                        null,
                        null,
                        null,
                        List.of(new FunnelStepResponse("PRODUCT_VIEW", 10, 1, 0))));

        mockMvc.perform(get("/api/v1/admin/analytics/funnel")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z")
                        .param("placement", "HOME_BEST_SELLERS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps[0].step").value("PRODUCT_VIEW"));

        verify(funnelAnalyticsService).summarize(
                from, to, null, null, null, "HOME_BEST_SELLERS", null);
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void funnel_customerCannotReadSummary() throws Exception {
        mockMvc.perform(get("/api/v1/admin/analytics/funnel")
                        .param("from", "2026-07-01T00:00:00Z")
                        .param("to", "2026-08-01T00:00:00Z"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(funnelAnalyticsService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void anonymize_adminCanRunRetentionPolicy() throws Exception {
        Instant cutoff = Instant.parse("2026-05-01T00:00:00Z");
        when(analyticsEventService.anonymizeExpiredRawEvents(any()))
                .thenReturn(new AnalyticsRetentionResponse(cutoff, 12, 3, 9));

        mockMvc.perform(post("/api/v1/admin/analytics/retention/anonymize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnalyticsRetentionRequest(90))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.eventsAnonymized").value(12))
                .andExpect(jsonPath("$.data.aggregateRowsUpdated").value(3))
                .andExpect(jsonPath("$.data.rawEventsDeleted").value(9));

        verify(analyticsEventService).anonymizeExpiredRawEvents(any());
    }
}
