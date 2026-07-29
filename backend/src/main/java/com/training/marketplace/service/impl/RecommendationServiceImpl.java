package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.dto.response.ProductVariantResponse;
import com.training.marketplace.dto.response.RecommendationItemResponse;
import com.training.marketplace.dto.response.RecommendationResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.mapper.ProductMapper;
import com.training.marketplace.mapper.ProductVariantMapper;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.service.RecommendationCandidate;
import com.training.marketplace.service.RecommendationContext;
import com.training.marketplace.service.RecommendationService;
import com.training.marketplace.service.RecommendationStrategy;
import com.training.marketplace.service.RecommendationStrategyRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RecommendationServiceImpl implements RecommendationService {

    static final int MAX_LIMIT = 24;
    static final int MAX_SESSION_ID_LENGTH = 128;

    private final RecommendationStrategyRegistry strategyRegistry;
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductMapper productMapper;
    private final ProductVariantMapper variantMapper;

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse recommend(
            Long userId,
            String sessionId,
            RecommendationPlacement placement,
            Long productId,
            Long categoryId,
            int limit) {
        validate(placement, productId, categoryId, limit);
        String normalizedSessionId = normalizeSessionId(sessionId);
        RecommendationStrategyType strategyType = strategyFor(placement);
        RecommendationContext context = new RecommendationContext(
                userId,
                normalizedSessionId,
                sourceProductId(placement, productId),
                categoryId(placement, categoryId),
                limit);
        RecommendationStrategy strategy = strategyRegistry.resolve(strategyType);
        List<RecommendationCandidate> candidates = strategy.recommend(context);

        return new RecommendationResponse(
                UUID.randomUUID(),
                placement,
                strategyType,
                Instant.now(),
                hydrate(candidates));
    }

    private List<RecommendationItemResponse> hydrate(
            List<RecommendationCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<Long> productIds = new LinkedHashSet<>();
        candidates.forEach(candidate -> productIds.add(candidate.productId()));

        Map<Long, Product> productsById = new HashMap<>();
        productRepository.findAllByIdInAndActiveTrue(productIds)
                .forEach(product -> productsById.put(product.getId(), product));

        Map<Long, List<ProductVariantResponse>> variantsByProductId =
                activeVariantsByProductId(productIds);
        List<RecommendationItemResponse> items = new ArrayList<>();
        for (RecommendationCandidate candidate : candidates) {
            Product product = productsById.get(candidate.productId());
            List<ProductVariantResponse> variants =
                    variantsByProductId.getOrDefault(candidate.productId(), List.of());
            if (product == null || variants.isEmpty()) {
                continue;
            }

            ProductResponse response = productMapper.toResponse(product).withVariants(variants);
            items.add(new RecommendationItemResponse(
                    items.size(),
                    response,
                    candidate.score(),
                    candidate.reason()));
        }
        return List.copyOf(items);
    }

    private Map<Long, List<ProductVariantResponse>> activeVariantsByProductId(
            Collection<Long> productIds) {
        Map<Long, List<ProductVariantResponse>> result = new HashMap<>();
        for (ProductVariant variant : variantRepository.findByProductIdInAndActiveTrue(productIds)) {
            result.computeIfAbsent(variant.getProductId(), ignored -> new ArrayList<>())
                    .add(variantMapper.toResponse(variant));
        }
        result.replaceAll((ignored, variants) -> List.copyOf(variants));
        return result;
    }

    private void validate(
            RecommendationPlacement placement,
            Long productId,
            Long categoryId,
            int limit) {
        if (placement == null) {
            throw new BadRequestException("Recommendation placement is required");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new BadRequestException(
                    "Recommendation limit must be between 1 and " + MAX_LIMIT);
        }
        switch (placement) {
            case PRODUCT_DETAIL_SIMILAR,
                    PRODUCT_DETAIL_CO_VIEWED,
                    PRODUCT_DETAIL_CO_PURCHASED -> requirePositive(
                            productId,
                            "Product ID is required for product-detail recommendations");
            case CATEGORY_BEST_SELLERS -> requirePositive(
                    categoryId,
                    "Category ID is required for category best sellers");
            case HOME_BEST_SELLERS -> {
                // No placement-specific identifier is required.
            }
        }
    }

    private void requirePositive(Long value, String missingMessage) {
        if (value == null) {
            throw new BadRequestException(missingMessage);
        }
        if (value <= 0) {
            throw new BadRequestException("Recommendation identifiers must be positive");
        }
    }

    private RecommendationStrategyType strategyFor(RecommendationPlacement placement) {
        return switch (placement) {
            case PRODUCT_DETAIL_SIMILAR -> RecommendationStrategyType.SIMILAR;
            case PRODUCT_DETAIL_CO_VIEWED -> RecommendationStrategyType.CO_VIEWED;
            case PRODUCT_DETAIL_CO_PURCHASED -> RecommendationStrategyType.CO_PURCHASED;
            case HOME_BEST_SELLERS, CATEGORY_BEST_SELLERS ->
                    RecommendationStrategyType.BEST_SELLER;
        };
    }

    private Long sourceProductId(
            RecommendationPlacement placement,
            Long productId) {
        return switch (placement) {
            case PRODUCT_DETAIL_SIMILAR,
                    PRODUCT_DETAIL_CO_VIEWED,
                    PRODUCT_DETAIL_CO_PURCHASED -> productId;
            case HOME_BEST_SELLERS, CATEGORY_BEST_SELLERS -> null;
        };
    }

    private Long categoryId(
            RecommendationPlacement placement,
            Long categoryId) {
        return placement == RecommendationPlacement.CATEGORY_BEST_SELLERS
                ? categoryId
                : null;
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        String normalized = sessionId.trim();
        if (normalized.length() > MAX_SESSION_ID_LENGTH) {
            throw new BadRequestException("X-Session-Id must not exceed 128 characters");
        }
        return normalized;
    }
}
