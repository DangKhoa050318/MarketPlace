package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateCategoryRequest;
import com.training.marketplace.dto.request.UpdateCategoryRequest;
import com.training.marketplace.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    List<CategoryResponse> getAll();
    CategoryResponse getById(Long id);
    CategoryResponse create(CreateCategoryRequest request);
    CategoryResponse update(Long id, UpdateCategoryRequest request);
    void delete(Long id);
}
