package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateProductRequest;
import com.training.marketplace.dto.request.ProductCatalogFilter;
import com.training.marketplace.dto.request.UpdateProductRequest;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.dto.response.StorefrontProductResponse;
import com.training.marketplace.dto.response.SuggestResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {
    Page<ProductResponse> getAll(Pageable pageable);
    Page<ProductResponse> search(String query, Pageable pageable);
    Page<StorefrontProductResponse> browse(ProductCatalogFilter filter, Pageable pageable);
    List<SuggestResult> suggest(String query, int limit);
    ProductResponse getById(Long id);        // returns SPU + its variants
    ProductResponse create(CreateProductRequest request);
    ProductResponse update(Long id, UpdateProductRequest request);
    void delete(Long id);                    // soft delete
}
