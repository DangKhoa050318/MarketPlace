package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.entity.Product;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.service.RecommendationCandidate;
import com.training.marketplace.service.RecommendationContext;
import com.training.marketplace.service.RecommendationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Serves precomputed, deterministic similar-product recommendations.
 *
 * <p>The expensive candidate discovery and scoring runs only when a source product has no
 * current precompute status. The request path otherwise reads a bounded Top-K set by indexed
 * source/rank and never materializes the active catalog in application memory.</p>
 */
@Service
@RequiredArgsConstructor
public class SimilarProductRecommendationStrategy implements RecommendationStrategy {

    static final String SIMILAR_CACHE = "similar-recommendations";

    private final ProductRepository productRepository;
    private final SimilarProductPrecomputeService precomputeService;

    @Override
    public RecommendationStrategyType getType() {
        return RecommendationStrategyType.SIMILAR;
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = SIMILAR_CACHE,
            key = "#context.productId() + ':' + #context.limit()",
            sync = true)
    public List<RecommendationCandidate> recommend(RecommendationContext context) {
        if (context.productId() == null) {
            throw new BadRequestException("Source product ID is required for SIMILAR recommendations");
        }

        Product source = productRepository.findById(context.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", context.productId()));
        if (!source.isActive()) {
            throw new BadRequestException(
                    "Source product is not active: " + context.productId());
        }

        return precomputeService.getOrCompute(context.productId(), context.limit());
    }
}
