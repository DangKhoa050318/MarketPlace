package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateProductVariantRequest;
import com.training.marketplace.dto.request.UpdateProductVariantRequest;
import com.training.marketplace.dto.response.ProductVariantResponse;

import java.util.List;

public interface ProductVariantService {
    List<ProductVariantResponse> listByProduct(Long productId);
    ProductVariantResponse getById(Long id);
    ProductVariantResponse create(Long productId, CreateProductVariantRequest request);
    ProductVariantResponse update(Long id, UpdateProductVariantRequest request);
    void delete(Long id);   // soft delete
}
