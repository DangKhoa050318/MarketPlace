package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.entity.Product;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationSourceProductValidator {

    private final ProductRepository productRepository;

    public Product requireActive(
            Long productId,
            RecommendationStrategyType strategyType) {
        if (productId == null) {
            throw new BadRequestException(
                    "Source product ID is required for " + strategyType + " recommendations");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        if (!product.isActive()) {
            throw new BadRequestException("Source product is not active: " + productId);
        }
        return product;
    }
}
