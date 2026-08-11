package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.CreateProductVariantRequest;
import com.training.marketplace.dto.request.UpdateProductVariantRequest;
import com.training.marketplace.dto.response.ProductVariantResponse;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.ProductVariantMapper;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.service.ProductVariantService;
import com.training.marketplace.service.ProductCatalogChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper variantMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariantResponse> listByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        return variantMapper.toResponseList(variantRepository.findByProductId(productId));
    }

    @Override
    @Transactional(readOnly = true)
    public ProductVariantResponse getById(Long id) {
        return variantMapper.toResponse(variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", id)));
    }

    @Override
    @Transactional
    public ProductVariantResponse create(Long productId, CreateProductVariantRequest request) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }
        if (variantRepository.existsBySku(request.sku())) {
            throw new DuplicateResourceException("ProductVariant", "sku", request.sku());
        }
        if (variantRepository.existsByProductIdAndVariantName(productId, request.variantName())) {
            throw new DuplicateResourceException("ProductVariant", "variantName", request.variantName());
        }
        ProductVariant variant = variantMapper.toEntity(request);
        variant.setProductId(productId);
        variant.setActive(true);
        ProductVariant saved = variantRepository.save(variant);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(saved.getProductId()));
        return variantMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public ProductVariantResponse update(Long id, UpdateProductVariantRequest request) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", id));
        variantMapper.updateEntity(variant, request);
        ProductVariant saved = variantRepository.save(variant);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(saved.getProductId()));
        return variantMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", id));
        variant.setActive(false);
        variantRepository.save(variant);
        eventPublisher.publishEvent(new ProductCatalogChangedEvent(variant.getProductId()));
    }
}
