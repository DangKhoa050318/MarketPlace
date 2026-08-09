package com.training.marketplace.controller;

import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.dto.response.RecommendationResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.Role;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.security.SecurityConfig;
import com.training.marketplace.service.RecommendationService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.training.marketplace.service.UserService;

@WebMvcTest(RecommendationController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class RecommendationControllerTest {

    private static final UUID REQUEST_ID =
            UUID.fromString("93fc3727-47ae-4cbe-88ee-f9934753deca");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;
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
    void recommend_anonymousStorefrontRequestIsPublicAndReturnsTrackingContext()
            throws Exception {
        when(recommendationService.recommend(
                null,
                "session-1",
                RecommendationPlacement.PRODUCT_DETAIL_SIMILAR,
                10L,
                null,
                12))
                .thenReturn(response(
                        RecommendationPlacement.PRODUCT_DETAIL_SIMILAR,
                        RecommendationStrategyType.SIMILAR));

        mockMvc.perform(get("/api/v1/recommendations")
                        .header("X-Session-Id", "session-1")
                        .param("placement", "PRODUCT_DETAIL_SIMILAR")
                        .param("productId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.requestId").value(REQUEST_ID.toString()))
                .andExpect(jsonPath("$.data.placement").value("PRODUCT_DETAIL_SIMILAR"))
                .andExpect(jsonPath("$.data.strategy").value("SIMILAR"))
                .andExpect(jsonPath("$.data.items").isArray());

        verify(recommendationService).recommend(
                null,
                "session-1",
                RecommendationPlacement.PRODUCT_DETAIL_SIMILAR,
                10L,
                null,
                12);
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    void recommend_authenticatedRequestDerivesUserFromPrincipal() throws Exception {
        User user = User.builder()
                .username("customer")
                .email("customer@example.com")
                .password("password")
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        user.setId(7L);
        when(userService.getAuthenticatedUser(any())).thenReturn(user);
        when(recommendationService.recommend(
                7L,
                null,
                RecommendationPlacement.HOME_BEST_SELLERS,
                null,
                null,
                6))
                .thenReturn(response(
                        RecommendationPlacement.HOME_BEST_SELLERS,
                        RecommendationStrategyType.BEST_SELLER));
        var authentication = new UsernamePasswordAuthenticationToken(
                "customer",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));

        mockMvc.perform(get("/api/v1/recommendations")
                        .principal(authentication)
                        .param("placement", "HOME_BEST_SELLERS")
                        .param("limit", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.strategy").value("BEST_SELLER"));

        verify(recommendationService).recommend(
                7L,
                null,
                RecommendationPlacement.HOME_BEST_SELLERS,
                null,
                null,
                6);
    }

    @Test
    void recommend_missingPlacementReturns400Envelope() throws Exception {
        when(recommendationService.recommend(null, null, null, null, null, 12))
                .thenThrow(new BadRequestException("Recommendation placement is required"));

        mockMvc.perform(get("/api/v1/recommendations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Recommendation placement is required"));

        verify(recommendationService).recommend(null, null, null, null, null, 12);
        verifyNoInteractions(userService);
    }

    @Test
    void recommend_unsupportedPlacementReturns400BeforeCallingService() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations")
                        .param("placement", "NOT_A_PLACEMENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Unsupported recommendation placement: NOT_A_PLACEMENT"));

        verifyNoInteractions(recommendationService);
    }

    private RecommendationResponse response(
            RecommendationPlacement placement,
            RecommendationStrategyType strategy) {
        return new RecommendationResponse(
                REQUEST_ID,
                placement,
                strategy,
                Instant.parse("2026-07-29T03:00:00Z"),
                List.of());
    }
}
