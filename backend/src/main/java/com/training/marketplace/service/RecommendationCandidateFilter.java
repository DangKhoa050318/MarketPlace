package com.training.marketplace.service;

import com.training.marketplace.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Applies storefront eligibility rules after an algorithm has ranked candidates.
 *
 * <p>The first occurrence of a product wins, preserving strategy ranking while
 * removing duplicates. Product visibility and sellability are resolved in one
 * batch query so all strategies share the same final eligibility boundary.</p>
 */
@Component
@RequiredArgsConstructor
public class RecommendationCandidateFilter {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<RecommendationCandidate> filter(
            List<RecommendationCandidate> candidates,
            Long sourceProductId,
            int limit) {
        Objects.requireNonNull(candidates, "Recommendation candidates are required");
        if (limit <= 0) {
            throw new IllegalArgumentException("Recommendation limit must be positive");
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> candidateIds = new LinkedHashSet<>();
        for (RecommendationCandidate candidate : candidates) {
            Objects.requireNonNull(candidate, "Recommendation candidate must not be null");
            if (!candidate.productId().equals(sourceProductId)) {
                candidateIds.add(candidate.productId());
            }
        }
        if (candidateIds.isEmpty()) {
            return List.of();
        }

        Set<Long> eligibleIds = new HashSet<>(
                productRepository.findRecommendationEligibleProductIds(candidateIds));
        Set<Long> emittedIds = new HashSet<>();
        return candidates.stream()
                .filter(candidate -> !candidate.productId().equals(sourceProductId))
                .filter(candidate -> eligibleIds.contains(candidate.productId()))
                .filter(candidate -> emittedIds.add(candidate.productId()))
                .limit(limit)
                .toList();
    }
}
