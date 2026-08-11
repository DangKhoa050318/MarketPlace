package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.CreateCategoryRequest;
import com.training.marketplace.dto.request.UpdateCategoryRequest;
import com.training.marketplace.dto.response.CategoryResponse;
import com.training.marketplace.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Category CRUD operations")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "Get all categories")
    public ApiResponse<List<CategoryResponse>> getAll() {
        return ApiResponse.success(categoryService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get category by ID")
    public ApiResponse<CategoryResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(categoryService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new category (MANAGER/ADMIN)")
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.success("Category created", categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update an existing category (MANAGER/ADMIN)")
    public ApiResponse<CategoryResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.success("Category updated", categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a category (ADMIN only)")
    public void delete(@PathVariable Long id) {
        categoryService.delete(id);
    }
}
