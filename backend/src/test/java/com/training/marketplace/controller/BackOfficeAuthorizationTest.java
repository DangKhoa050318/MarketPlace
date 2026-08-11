package com.training.marketplace.controller;

import com.training.marketplace.observability.CoreFeatureRequestFilter;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.security.SecurityConfig;
import com.training.marketplace.service.AuditService;
import com.training.marketplace.service.BannerService;
import com.training.marketplace.service.CampaignService;
import com.training.marketplace.service.CategoryService;
import com.training.marketplace.service.CollectionService;
import com.training.marketplace.service.MerchandisingEventService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AdminCampaignController.class,
        AdminCollectionController.class,
        AdminBannerController.class,
        AdminMerchandisingController.class,
        CategoryController.class
})
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class BackOfficeAuthorizationTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private CampaignService campaignService;
    @MockBean private CollectionService collectionService;
    @MockBean private BannerService bannerService;
    @MockBean private MerchandisingEventService merchandisingEventService;
    @MockBean private CategoryService categoryService;
    @MockBean private AuditService auditService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private RateLimitingFilter rateLimitingFilter;
    @MockBean private CoreFeatureRequestFilter coreFeatureRequestFilter;

    @BeforeEach
    void setUp() throws Exception {
        passThrough(jwtAuthenticationFilter);
        passThrough(rateLimitingFilter);
        passThrough(coreFeatureRequestFilter);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/admin/campaigns",
            "/api/v1/admin/collections",
            "/api/v1/admin/banners",
            "/api/v1/admin/merchandising/summary?targetType=CAMPAIGN&targetId=1"
                    + "&from=2026-08-01T00:00:00&to=2026-08-02T00:00:00"
    })
    void adminEndpoints_anonymousUserGetsUnauthorized(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/admin/campaigns",
            "/api/v1/admin/collections",
            "/api/v1/admin/banners",
            "/api/v1/admin/merchandising/summary?targetType=CAMPAIGN&targetId=1"
                    + "&from=2026-08-01T00:00:00&to=2026-08-02T00:00:00"
    })
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void adminEndpoints_customerGetsForbidden(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint))
                .andExpect(status().isForbidden());

        verifyNoInteractions(campaignService, collectionService, bannerService, merchandisingEventService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/v1/admin/campaigns",
            "/api/v1/admin/collections",
            "/api/v1/admin/banners",
            "/api/v1/admin/merchandising/summary?targetType=CAMPAIGN&targetId=1"
                    + "&from=2026-08-01T00:00:00&to=2026-08-02T00:00:00"
    })
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminEndpoints_adminIsAllowed(String endpoint) throws Exception {
        mockMvc.perform(get(endpoint))
                .andExpect(status().isOk());
    }

    @Test
    void adminControllers_declareDefenseInDepthAuthorization() {
        assertAdminOnly(AdminCampaignController.class);
        assertAdminOnly(AdminCollectionController.class);
        assertAdminOnly(AdminBannerController.class);
        assertAdminOnly(AdminMerchandisingController.class);
    }

    @Test
    void categoryDelete_anonymousUserGetsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/1"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(categoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "MANAGER", "STAFF"})
    void categoryDelete_nonAdminRoleGetsForbidden(String role) throws Exception {
        mockMvc.perform(delete("/api/v1/categories/1")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                                .user(role.toLowerCase()).roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(categoryService);
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void categoryDelete_adminIsAllowed() throws Exception {
        long categoryId = 1L;
        mockMvc.perform(delete("/api/v1/categories/{id}", categoryId))
                .andExpect(status().isNoContent());

        verify(categoryService).delete(categoryId);
    }

    private void assertAdminOnly(Class<?> controllerType) {
        PreAuthorize annotation = controllerType.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("%s must keep controller-level authorization", controllerType.getSimpleName())
                .isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }

    private void passThrough(org.springframework.web.filter.OncePerRequestFilter filter) throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(filter).doFilter(any(), any(), any());
    }
}
