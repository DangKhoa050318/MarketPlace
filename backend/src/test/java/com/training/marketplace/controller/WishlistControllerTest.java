package com.training.marketplace.controller;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.dto.response.WishlistItemResponse;
import com.training.marketplace.dto.response.WishlistStatusResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.Role;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.security.SecurityConfig;
import com.training.marketplace.service.UserService;
import com.training.marketplace.service.WishlistService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WishlistController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class WishlistControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private WishlistService wishlistService;
    @MockBean private UserService userService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private RateLimitingFilter rateLimitingFilter;

    private UsernamePasswordAuthenticationToken auth;

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

        User user = User.builder()
                .username("customer")
                .email("customer@example.com")
                .password("password")
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        user.setId(7L);
        when(userService.getAuthenticatedUser(any())).thenReturn(user);
        auth = new UsernamePasswordAuthenticationToken(
                "customer",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void list_authenticatedUser_returnsPagedWishlist() throws Exception {
        when(wishlistService.list(7L, 0, 12)).thenReturn(PageResponse.<WishlistItemResponse>builder()
                .content(List.of(item()))
                .page(0)
                .size(12)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build());

        mockMvc.perform(get("/api/v1/wishlist").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].product.id").value(10))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void add_duplicateProductIsIdempotentAtServiceBoundary() throws Exception {
        when(wishlistService.add(7L, 10L)).thenReturn(item());

        mockMvc.perform(post("/api/v1/wishlist/10").principal(auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.product.id").value(10));

        verify(wishlistService).add(7L, 10L);
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void status_authenticatedUser_returnsWishlistState() throws Exception {
        when(wishlistService.status(7L, 10L)).thenReturn(new WishlistStatusResponse(10L, true));

        mockMvc.perform(get("/api/v1/wishlist/10/status").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.wishlisted").value(true));
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void remove_authenticatedUser_deletesByCurrentUser() throws Exception {
        mockMvc.perform(delete("/api/v1/wishlist/10").principal(auth))
                .andExpect(status().isNoContent());

        verify(wishlistService).remove(7L, 10L);
    }

    @Test
    void list_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/wishlist"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(wishlistService);
    }

    private WishlistItemResponse item() {
        ProductResponse product = new ProductResponse(
                10L,
                "phone",
                "Phone",
                "Desc",
                1L,
                "PCS",
                "http://example.com/phone.png",
                true,
                LocalDateTime.now(),
                LocalDateTime.now(),
                List.of());
        return new WishlistItemResponse(100L, 7L, LocalDateTime.now(), product);
    }
}
