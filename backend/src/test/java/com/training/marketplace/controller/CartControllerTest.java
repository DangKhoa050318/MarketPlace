package com.training.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.dto.request.AddToCartRequest;
import com.training.marketplace.dto.request.UpdateCartItemRequest;
import com.training.marketplace.dto.response.CartItemResponse;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.Role;
import com.training.marketplace.service.UserService;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.security.SecurityConfig;
import com.training.marketplace.service.CartService;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    private User mockUser;
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

        mockUser = User.builder()
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .role(Role.CUSTOMER)
                .active(true)
                .build();
        mockUser.setId(1L);

        when(userService.getAuthenticatedUser(any())).thenReturn(mockUser);
        auth = new UsernamePasswordAuthenticationToken("testuser", "password", List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CUSTOMER"})
    void getCart_validUser_returns200AndCartResponse() throws Exception {
        // Given
        var cartItem = new CartItemResponse(10L, 100L, "SKU-10", "Phone", "Silver", BigDecimal.valueOf(500), 2, BigDecimal.valueOf(1000), "http://img.jpg");
        var cartResponse = new CartResponse(1L, List.of(cartItem), BigDecimal.valueOf(1000), BigDecimal.ZERO, 2);
        when(cartService.getCart(1L)).thenReturn(cartResponse);

        // When & Then
        mockMvc.perform(get("/api/v1/cart").principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.totalItems").value(2))
                .andExpect(jsonPath("$.data.items[0].productName").value("Phone"));
    }

    @Test
    void getCart_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/cart"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CUSTOMER"})
    void addItem_validRequest_returns200AndCartResponse() throws Exception {
        // Given
        var request = new AddToCartRequest(10L, 2);
        var cartItem = new CartItemResponse(10L, 100L, "SKU-10", "Phone", "Silver", BigDecimal.valueOf(500), 2, BigDecimal.valueOf(1000), "http://img.jpg");
        var cartResponse = new CartResponse(1L, List.of(cartItem), BigDecimal.valueOf(1000), BigDecimal.ZERO, 2);

        when(cartService.addItem(eq(1L), any(AddToCartRequest.class))).thenReturn(cartResponse);

        // When & Then
        mockMvc.perform(post("/api/v1/cart/items")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Item added to cart"))
                .andExpect(jsonPath("$.data.totalItems").value(2));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CUSTOMER"})
    void updateQuantity_validRequest_returns200AndCartResponse() throws Exception {
        // Given
        var request = new UpdateCartItemRequest(3);
        var cartItem = new CartItemResponse(10L, 100L, "SKU-10", "Phone", "Silver", BigDecimal.valueOf(500), 3, BigDecimal.valueOf(1500), "http://img.jpg");
        var cartResponse = new CartResponse(1L, List.of(cartItem), BigDecimal.valueOf(1500), BigDecimal.ZERO, 3);

        when(cartService.updateQuantity(eq(1L), eq(10L), eq(3))).thenReturn(cartResponse);

        // When & Then
        mockMvc.perform(put("/api/v1/cart/items/{productId}", 10L)
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cart updated"))
                .andExpect(jsonPath("$.data.totalItems").value(3));
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CUSTOMER"})
    void removeItem_existingProduct_returns204NoContent() throws Exception {
        // Given
        doNothing().when(cartService).removeItem(1L, 10L);

        // When & Then
        mockMvc.perform(delete("/api/v1/cart/items/{productId}", 10L).principal(auth))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"CUSTOMER"})
    void clearCart_validUser_returns204NoContent() throws Exception {
        // Given
        doNothing().when(cartService).clearCart(1L);

        // When & Then
        mockMvc.perform(delete("/api/v1/cart").principal(auth))
                .andExpect(status().isNoContent());
    }
}
