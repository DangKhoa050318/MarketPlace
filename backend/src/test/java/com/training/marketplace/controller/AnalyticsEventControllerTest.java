package com.training.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.analytics.AnalyticsEventType;
import com.training.marketplace.analytics.AnalyticsIngestionStatus;
import com.training.marketplace.dto.request.TrackAnalyticsEventBatchRequest;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.dto.response.AnalyticsBatchIngestionResponse;
import com.training.marketplace.dto.response.AnalyticsEventResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.Role;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.security.SecurityConfig;
import com.training.marketplace.service.AnalyticsEventService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.training.marketplace.service.UserService;

@WebMvcTest(AnalyticsEventController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class AnalyticsEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AnalyticsEventService analyticsEventService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

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
    void track_anonymousEvent_returns202AndPassesSessionHeader() throws Exception {
        UUID eventId = UUID.fromString("cbe865ca-3c3c-4dc6-b5cb-a30833342848");
        Instant receivedAt = Instant.parse("2026-07-28T12:30:01Z");
        var request = browserProductView(eventId);
        var response = acceptedResponse(eventId, null, "session-1", receivedAt);
        when(analyticsEventService.track(eq(null), eq("session-1"), any()))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/analytics/events")
                        .header("X-Session-Id", "session-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.receivedAt").value("2026-07-28T12:30:01Z"));

        verify(analyticsEventService).track(eq(null), eq("session-1"), any());
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void track_authenticatedEvent_derivesUserIdFromPrincipal() throws Exception {
        User user = User.builder()
                .username("customer")
                .email("customer@example.com")
                .password("password")
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        user.setId(7L);
        when(userService.getAuthenticatedUser(any())).thenReturn(user);

        UUID eventId = UUID.fromString("cbe865ca-3c3c-4dc6-b5cb-a30833342848");
        var request = browserProductView(eventId);
        when(analyticsEventService.track(eq(7L), eq("session-1"), any()))
                .thenReturn(acceptedResponse(
                        eventId,
                        7L,
                        "session-1",
                        Instant.parse("2026-07-28T12:30:01Z")));
        var authentication = new UsernamePasswordAuthenticationToken(
                "customer",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        mockMvc.perform(post("/api/v1/analytics/events")
                        .principal(authentication)
                        .header("X-Session-Id", "session-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());

        verify(analyticsEventService).track(eq(7L), eq("session-1"), any());
    }

    @Test
    void track_missingEventId_returns400BeforeCallingService() throws Exception {
        String payload = """
                {
                  "schemaVersion": 1,
                  "type": "PRODUCT_VIEW",
                  "occurredAt": "2026-07-28T12:30:00Z",
                  "productId": 10
                }
                """;

        mockMvc.perform(post("/api/v1/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data.eventId").value("Event ID is required"));

        verifyNoInteractions(analyticsEventService);
    }

    @Test
    void track_unsupportedEventType_returns400BeforeCallingService() throws Exception {
        String payload = """
                {
                  "eventId": "cbe865ca-3c3c-4dc6-b5cb-a30833342848",
                  "schemaVersion": 1,
                  "type": "NOT_A_REAL_EVENT",
                  "occurredAt": "2026-07-28T12:30:00Z"
                }
                """;

        mockMvc.perform(post("/api/v1/analytics/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Malformed JSON or unsupported event value"));

        verifyNoInteractions(analyticsEventService);
    }

    @Test
    void trackBatch_validEnvelopeReturnsAcceptedStatusSummary() throws Exception {
        UUID eventId = UUID.fromString("cbe865ca-3c3c-4dc6-b5cb-a30833342848");
        var response = acceptedResponse(eventId, null, "session-1", Instant.parse("2026-07-28T12:30:01Z"));
        when(analyticsEventService.trackBatch(eq(null), eq("session-1"), any()))
                .thenReturn(new AnalyticsBatchIngestionResponse(1, 0, 1, List.of(response)));

        mockMvc.perform(post("/api/v1/analytics/events/batch")
                        .header("X-Session-Id", "session-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TrackAnalyticsEventBatchRequest(List.of(browserProductView(eventId))))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.accepted").value(1))
                .andExpect(jsonPath("$.data.duplicates").value(0))
                .andExpect(jsonPath("$.data.events[0].status").value("ACCEPTED"));

        verify(analyticsEventService).trackBatch(eq(null), eq("session-1"), any());
    }

    private TrackAnalyticsEventRequest browserProductView(UUID eventId) {
        return new TrackAnalyticsEventRequest(
                eventId,
                1,
                AnalyticsEventType.PRODUCT_VIEW,
                Instant.parse("2026-07-28T12:30:00Z"),
                10L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Map.of());
    }

    private AnalyticsEventResponse acceptedResponse(
            UUID eventId,
            Long userId,
            String sessionId,
            Instant receivedAt) {
        return new AnalyticsEventResponse(
                AnalyticsEventType.PRODUCT_VIEW,
                userId,
                sessionId,
                LocalDateTime.of(2026, 7, 28, 12, 30),
                Map.of(),
                eventId,
                AnalyticsIngestionStatus.ACCEPTED,
                receivedAt);
    }
}
