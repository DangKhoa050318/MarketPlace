package com.training.marketplace.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.dto.request.CreateProductRequest;
import com.training.marketplace.dto.request.UpdateProductRequest;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.dto.response.StorefrontProductResponse;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.security.JwtAuthenticationFilter;
import com.training.marketplace.security.RateLimitingFilter;
import com.training.marketplace.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ProductService productService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean private RateLimitingFilter rateLimitingFilter;

    private ProductResponse sampleResponse(Long id, String slug, String name) {
        return new ProductResponse(id, slug, name, "A laptop", 1L, "PCS",
                "http://example.com/image.png", true, LocalDateTime.now(), LocalDateTime.now(), null);
    }

    @Test
    void getAll_validRequest_returnsPageOfProducts() throws Exception {
        var page = new PageImpl<>(List.of(sampleResponse(1L, "laptop", "Laptop")), PageRequest.of(0, 20), 1);
        when(productService.getAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/products")
                        .param("page", "0").param("size", "20")
                        .param("sortBy", "createdAt").param("sortDir", "DESC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Laptop"))
                .andExpect(jsonPath("$.data.content[0].slug").value("laptop"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    void browseCatalog_returnsPriceAndAvailability() throws Exception {
        var item = new StorefrontProductResponse(1L, "laptop", "Laptop", "A laptop",
                1L, "Computers", "PCS", "http://example.com/image.png",
                BigDecimal.valueOf(999), BigDecimal.valueOf(1299), 12, 2, LocalDateTime.now());
        var page = new PageImpl<>(List.of(item), PageRequest.of(0, 12), 1);
        when(productService.browse(any(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/products/catalog").param("inStock", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].minPrice").value(999))
                .andExpect(jsonPath("$.data.content[0].availableStock").value(12));
    }

    @Test
    void browseCatalog_invalidPriceRange_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/products/catalog")
                        .param("minPrice", "500").param("maxPrice", "100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getById_found_returnsProduct() throws Exception {
        when(productService.getById(1L)).thenReturn(sampleResponse(1L, "laptop", "Laptop"));

        mockMvc.perform(get("/api/v1/products/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Laptop"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        when(productService.getById(999L)).thenThrow(new ResourceNotFoundException("Product", 999L));

        mockMvc.perform(get("/api/v1/products/{id}", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void create_validRequest_returns201AndProduct() throws Exception {
        var request = new CreateProductRequest("laptop", "Laptop", "A laptop", 1L, "PCS", "http://example.com/image.png");
        when(productService.create(any(CreateProductRequest.class))).thenReturn(sampleResponse(1L, "laptop", "Laptop"));

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.message").value("Product created"));
    }

    @Test
    void create_invalidRequest_returns400() throws Exception {
        // blank slug & name, null categoryId
        var request = new CreateProductRequest("", "", "A laptop", null, "PCS", "http://example.com/image.png");

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void update_validRequest_returns200AndProduct() throws Exception {
        var request = new UpdateProductRequest("Laptop Pro", "A pro laptop", 1L, "PCS", "http://example.com/image.png", true);
        when(productService.update(eq(1L), any(UpdateProductRequest.class))).thenReturn(sampleResponse(1L, "laptop", "Laptop Pro"));

        mockMvc.perform(put("/api/v1/products/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Laptop Pro"))
                .andExpect(jsonPath("$.message").value("Product updated"));
    }

    @Test
    void delete_existingProduct_returns204() throws Exception {
        doNothing().when(productService).delete(1L);
        mockMvc.perform(delete("/api/v1/products/{id}", 1L))
                .andExpect(status().isNoContent());
    }
}
