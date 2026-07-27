package com.training.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.dto.request.CreateCategoryRequest;
import com.training.marketplace.dto.response.CategoryResponse;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private RateLimitingFilter rateLimitingFilter;

    @Test
    void getAll_validRequest_returnsListOfCategories() throws Exception {
        // Given
        var response = new CategoryResponse(1L, "Electronics", "ELEC", "electronics", null, LocalDateTime.now(), LocalDateTime.now());
        when(categoryService.getAll()).thenReturn(List.of(response));

        // When & Then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Electronics"))
                .andExpect(jsonPath("$.data[0].slug").value("electronics"));
    }

    @Test
    void create_validRequest_returns201AndCategory() throws Exception {
        // Given
        var request = new CreateCategoryRequest("Electronics", "ELEC", "electronics", null);
        var response = new CategoryResponse(1L, "Electronics", "ELEC", "electronics", null, LocalDateTime.now(), LocalDateTime.now());
        when(categoryService.create(any(CreateCategoryRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.message").value("Category created"));
    }

    @Test
    void create_invalidRequest_returns400() throws Exception {
        // Given - empty name
        var request = new CreateCategoryRequest("", "", "", null);

        // When & Then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
