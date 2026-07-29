package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationStrategyType;
import com.training.marketplace.entity.Product;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.projection.ProductPriceRangeProjection;
import com.training.marketplace.service.RecommendationCandidate;
import com.training.marketplace.service.RecommendationContext;
import com.training.marketplace.service.RecommendationStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic rule-based similar-product strategy.
 *
 * <p>Similarity is weighted by category (35%), brand (25%), exact normalized
 * attribute overlap (25%), and active-SKU price-range proximity (15%). A candidate
 * must share at least one semantic signal (category, brand, or attribute); price
 * alone never makes unrelated products similar.</p>
 */
@Service
@RequiredArgsConstructor
public class SimilarProductRecommendationStrategy implements RecommendationStrategy {

    static final double CATEGORY_WEIGHT = 0.35;
    static final double BRAND_WEIGHT = 0.25;
    static final double ATTRIBUTE_WEIGHT = 0.25;
    static final double PRICE_WEIGHT = 0.15;
    static final double SIMILAR_PRICE_THRESHOLD = 0.50;

    private static final Comparator<RecommendationCandidate> RANKING =
            Comparator.comparingDouble(RecommendationCandidate::score)
                    .reversed()
                    .thenComparing(RecommendationCandidate::productId);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public RecommendationStrategyType getType() {
        return RecommendationStrategyType.SIMILAR;
    }

    @Override
    @Transactional(readOnly = true)
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

        List<Product> activeProducts = productRepository.findAllByActiveTrue();
        Map<Long, PriceRange> priceRanges = loadPriceRanges(activeProducts, source.getId());
        PriceRange sourcePriceRange = priceRanges.get(source.getId());

        return activeProducts.stream()
                .filter(Product::isActive)
                .filter(candidate -> !candidate.getId().equals(source.getId()))
                .map(candidate -> score(source, candidate, sourcePriceRange,
                        priceRanges.get(candidate.getId())))
                .filter(scored -> scored.semanticMatch())
                .map(ScoredCandidate::candidate)
                .sorted(RANKING)
                .limit(context.limit())
                .toList();
    }

    private Map<Long, PriceRange> loadPriceRanges(
            Collection<Product> activeProducts,
            Long sourceProductId) {
        Set<Long> productIds = new HashSet<>();
        productIds.add(sourceProductId);
        activeProducts.stream()
                .map(Product::getId)
                .forEach(productIds::add);

        Map<Long, PriceRange> result = new HashMap<>();
        for (ProductPriceRangeProjection range
                : productVariantRepository.findActivePriceRanges(productIds)) {
            result.put(
                    range.getProductId(),
                    new PriceRange(range.getMinPrice(), range.getMaxPrice()));
        }
        return result;
    }

    private ScoredCandidate score(
            Product source,
            Product candidate,
            PriceRange sourcePriceRange,
            PriceRange candidatePriceRange) {
        boolean sameCategory = source.getCategoryId() != null
                && source.getCategoryId().equals(candidate.getCategoryId());
        boolean sameBrand = normalizedEquals(source.getBrand(), candidate.getBrand());
        double attributeSimilarity = attributeSimilarity(
                source.getAttributes(),
                candidate.getAttributes());
        double priceSimilarity = priceSimilarity(sourcePriceRange, candidatePriceRange);

        double score = (sameCategory ? CATEGORY_WEIGHT : 0)
                + (sameBrand ? BRAND_WEIGHT : 0)
                + attributeSimilarity * ATTRIBUTE_WEIGHT
                + priceSimilarity * PRICE_WEIGHT;

        List<String> reasons = new ArrayList<>();
        if (sameCategory) {
            reasons.add("same_category");
        }
        if (sameBrand) {
            reasons.add("same_brand");
        }
        if (attributeSimilarity > 0) {
            reasons.add("matching_attributes");
        }
        if (priceSimilarity >= SIMILAR_PRICE_THRESHOLD) {
            reasons.add("similar_price");
        }

        boolean semanticMatch = sameCategory || sameBrand || attributeSimilarity > 0;
        return new ScoredCandidate(
                new RecommendationCandidate(
                        candidate.getId(),
                        score,
                        String.join(",", reasons)),
                semanticMatch);
    }

    private boolean normalizedEquals(String first, String second) {
        String normalizedFirst = normalize(first);
        return !normalizedFirst.isEmpty() && normalizedFirst.equals(normalize(second));
    }

    private double attributeSimilarity(
            Map<String, String> sourceAttributes,
            Map<String, String> candidateAttributes) {
        Set<String> source = normalizeAttributes(sourceAttributes);
        Set<String> candidate = normalizeAttributes(candidateAttributes);
        if (source.isEmpty() || candidate.isEmpty()) {
            return 0;
        }

        Set<String> intersection = new HashSet<>(source);
        intersection.retainAll(candidate);
        Set<String> union = new HashSet<>(source);
        union.addAll(candidate);
        return (double) intersection.size() / union.size();
    }

    private Set<String> normalizeAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new HashSet<>();
        attributes.forEach((key, value) -> {
            String normalizedKey = normalize(key);
            String normalizedValue = normalize(value);
            if (!normalizedKey.isEmpty() && !normalizedValue.isEmpty()) {
                normalized.add(normalizedKey + "=" + normalizedValue);
            }
        });
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private double priceSimilarity(PriceRange source, PriceRange candidate) {
        if (source == null || candidate == null || !source.isValid() || !candidate.isValid()) {
            return 0;
        }
        if (source.overlaps(candidate)) {
            return 1;
        }

        BigDecimal gap = source.maxPrice().compareTo(candidate.minPrice()) < 0
                ? candidate.minPrice().subtract(source.maxPrice())
                : source.minPrice().subtract(candidate.maxPrice());
        BigDecimal largestMidpoint = source.midpoint().max(candidate.midpoint());
        if (largestMidpoint.signum() <= 0) {
            return 0;
        }
        return Math.max(0, 1 - gap.doubleValue() / largestMidpoint.doubleValue());
    }

    private record PriceRange(BigDecimal minPrice, BigDecimal maxPrice) {

        private boolean isValid() {
            return minPrice != null
                    && maxPrice != null
                    && minPrice.signum() >= 0
                    && maxPrice.compareTo(minPrice) >= 0;
        }

        private boolean overlaps(PriceRange other) {
            return minPrice.compareTo(other.maxPrice) <= 0
                    && other.minPrice.compareTo(maxPrice) <= 0;
        }

        private BigDecimal midpoint() {
            return minPrice.add(maxPrice).divide(BigDecimal.valueOf(2));
        }
    }

    private record ScoredCandidate(
            RecommendationCandidate candidate,
            boolean semanticMatch) {
    }
}
