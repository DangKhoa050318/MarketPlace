package com.training.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.dto.request.CreateDeliveryRequest;
import com.training.marketplace.dto.request.UpdateDeliveryStatusRequest;
import com.training.marketplace.dto.response.DeliveryResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.DeliveryStatus;
import com.training.marketplace.enums.Role;
import com.training.marketplace.exception.ConflictException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.security.SecurityConfig;
import com.training.marketplace.service.DeliveryService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({DeliveryController.class, AdminDeliveryController.class})
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class DeliveryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private DeliveryService deliveryService;
    @MockBean private UserRepository userRepository;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private RateLimitingFilter rateLimitingFilter;

    private User customer;
    private User staff;
    private DeliveryResponse response;

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

        customer = User.builder().username("customer").email("customer@example.com")
                .password("encoded").role(Role.CUSTOMER).active(true).build();
        customer.setId(7L);
        staff = User.builder().username("staff").email("staff@example.com")
                .password("encoded").role(Role.STAFF).active(true).build();
        staff.setId(2L);

        response = new DeliveryResponse(
                20L, 10L, 0L, "GHN", "GHN-001", DeliveryStatus.PENDING,
                LocalDate.now().plusDays(2), null, List.of(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void customerCanReadOwnedOrderDelivery() throws Exception {
        when(userRepository.findByUsername("customer")).thenReturn(Optional.of(customer));
        when(deliveryService.getForCustomer(10L, 7L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/orders/10/delivery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.trackingCode").value("GHN-001"));
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void staffCanCreateDeliveryForShippedOrder() throws Exception {
        CreateDeliveryRequest request = new CreateDeliveryRequest(
                "GHN", "GHN-001", LocalDate.now().plusDays(2));
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(deliveryService.create(eq(10L), eq(2L), any(CreateDeliveryRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/orders/10/delivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(20));
    }

    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void customerCannotCreateDelivery() throws Exception {
        CreateDeliveryRequest request = new CreateDeliveryRequest(
                "GHN", "GHN-001", LocalDate.now().plusDays(2));

        mockMvc.perform(post("/api/v1/admin/orders/10/delivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void staleStatusCommandReturnsConflict() throws Exception {
        when(userRepository.findByUsername("staff")).thenReturn(Optional.of(staff));
        when(deliveryService.updateStatus(eq(20L), eq(2L), any(UpdateDeliveryStatusRequest.class)))
                .thenThrow(new ConflictException(
                        "Delivery was changed by another request; reload it before updating status"));

        mockMvc.perform(patch("/api/v1/admin/deliveries/20/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"status":"IN_TRANSIT"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(
                        "Delivery was changed by another request; reload it before updating status"));
    }

    @Test
    @WithMockUser(username = "staff", roles = "STAFF")
    void staffCannotUseCustomerOwnershipEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/orders/10/delivery"))
                .andExpect(status().isForbidden());
    }
}
