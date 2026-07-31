package com.training.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.analytics.AnalyticsExportStatus;
import com.training.marketplace.analytics.AnalyticsExportType;
import com.training.marketplace.dto.request.CreateAnalyticsExportRequest;
import com.training.marketplace.dto.response.AnalyticsExportJobResponse;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.security.SecurityConfig;
import com.training.marketplace.service.AnalyticsExportService;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminAnalyticsExportController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AdminAnalyticsExportControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AnalyticsExportService analyticsExportService;
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
    void create_returnsAcceptedJob() throws Exception {
        UUID id = UUID.randomUUID();
        when(analyticsExportService.create(any())).thenReturn(new AnalyticsExportJobResponse(
                id,
                AnalyticsExportType.OVERVIEW,
                AnalyticsExportStatus.PENDING,
                null,
                null,
                null,
                null,
                null));
        var request = new CreateAnalyticsExportRequest(
                AnalyticsExportType.OVERVIEW,
                Instant.parse("2026-07-01T00:00:00Z"),
                Instant.parse("2026-07-31T00:00:00Z"),
                null, null, null, null, null);

        mockMvc.perform(post("/api/v1/admin/analytics/exports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void download_returnsCsvAttachment() throws Exception {
        UUID id = UUID.randomUUID();
        byte[] csv = "productViews,orders\n10,2\n".getBytes(StandardCharsets.UTF_8);
        when(analyticsExportService.download(id))
                .thenReturn(new AnalyticsExportService.AnalyticsExportDownload("analytics.csv", csv));

        mockMvc.perform(get("/api/v1/admin/analytics/exports/{id}/download", id))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("analytics.csv")))
                .andExpect(content().bytes(csv));
    }
}
