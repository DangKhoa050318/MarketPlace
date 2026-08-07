package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.CreateProductRequest;
import com.training.marketplace.dto.request.ProductCatalogFilter;
import com.training.marketplace.dto.request.UpdateProductRequest;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.dto.response.StorefrontProductResponse;
import com.training.marketplace.dto.response.SuggestResult;
import com.training.marketplace.entity.Product;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.ProductMapper;
import com.training.marketplace.mapper.ProductVariantMapper;
import com.training.marketplace.repository.CategoryRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.StorefrontCatalogRepository;
import com.training.marketplace.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper variantMapper;
    private final StorefrontCatalogRepository storefrontCatalogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAll(Pageable pageable) {
        return productRepository.findAll(pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> search(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return getAll(pageable);
        }
        return productRepository.searchProducts(query.trim(), pageable).map(productMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StorefrontProductResponse> browse(ProductCatalogFilter filter, Pageable pageable) {
        return storefrontCatalogRepository.browse(filter, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SuggestResult> suggest(String query, int limit) {
        return storefrontCatalogRepository.suggest(query, limit);
    }

    @Override
    @Cacheable(value = "products-v2", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return productMapper.toResponse(product)
                .withVariants(variantMapper.toResponseList(variantRepository.findByProductId(id)));
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        if (productRepository.existsBySlug(request.slug())) {
            throw new DuplicateResourceException("Product", "slug", request.slug());
        }
        if (!categoryRepository.existsById(request.categoryId())) {
            throw new ResourceNotFoundException("Category", request.categoryId());
        }
        Product product = productMapper.toEntity(request);
        product.setActive(true);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = "products-v2", key = "#id")
    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.categoryId() != null && !categoryRepository.existsById(request.categoryId())) {
            throw new ResourceNotFoundException("Category", request.categoryId());
        }

        productMapper.updateEntity(product, request);
        return productMapper.toResponse(productRepository.save(product));
    }

    @Override
    @CacheEvict(value = "products-v2", key = "#id")
    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(false);
        productRepository.save(product);
    }
}
