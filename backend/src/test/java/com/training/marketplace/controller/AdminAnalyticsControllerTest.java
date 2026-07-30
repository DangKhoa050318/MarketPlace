package com.training.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.dto.request.AnalyticsRetentionRequest;
import com.training.marketplace.dto.response.AnalyticsRetentionResponse;
import com.training.marketplace.dto.response.FunnelStepResponse;
import com.training.marketplace.dto.response.FunnelSummaryResponse;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.security.SecurityConfig;
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
    void funnel_managerCanReadSummary() throws Exception {
        Instant from = Instant.parse("2026-07-01T00:00:00Z");
        Instant to = Instant.parse("2026-08-01T00:00:00Z");
        when(funnelAnalyticsService.summarize(from, to, null, null, null, null))
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
                        .param("to", "2026-08-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.steps[0].step").value("PRODUCT_VIEW"));
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
                .thenReturn(new AnalyticsRetentionResponse(cutoff, 12));

        mockMvc.perform(post("/api/v1/admin/analytics/retention/anonymize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnalyticsRetentionRequest(90))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.eventsAnonymized").value(12));

        verify(analyticsEventService).anonymizeExpiredRawEvents(any());
    }
}
