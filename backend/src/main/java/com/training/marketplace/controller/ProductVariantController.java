package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.CreateProductVariantRequest;
import com.training.marketplace.dto.request.UpdateProductVariantRequest;
import com.training.marketplace.dto.response.ProductVariantResponse;
import com.training.marketplace.service.ProductVariantService;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Product Variants", description = "Variant (SKU / mẫu mã) operations")
public class ProductVariantController {

    private final ProductVariantService variantService;

    @GetMapping("/products/{productId}/variants")
    @Operation(summary = "List variants of a product")
    public ApiResponse<List<ProductVariantResponse>> listByProduct(@PathVariable Long productId) {
        return ApiResponse.success(variantService.listByProduct(productId));
    }

    @PostMapping("/products/{productId}/variants")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a variant under a product (MANAGER/ADMIN)")
    public ApiResponse<ProductVariantResponse> create(
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductVariantRequest request) {
        return ApiResponse.success("Variant created", variantService.create(productId, request));
    }

    @GetMapping("/variants/{id}")
    @Operation(summary = "Get variant by id")
    public ApiResponse<ProductVariantResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(variantService.getById(id));
    }

    @PutMapping("/variants/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    @Operation(summary = "Update a variant (MANAGER/ADMIN)")
    public ApiResponse<ProductVariantResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductVariantRequest request) {
        return ApiResponse.success("Variant updated", variantService.update(id, request));
    }

    @DeleteMapping("/variants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Soft delete a variant (ADMIN)")
    public void delete(@PathVariable Long id) {
        variantService.delete(id);
    }
}
