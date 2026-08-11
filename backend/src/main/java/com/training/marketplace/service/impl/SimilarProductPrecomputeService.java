package com.training.marketplace.service.impl;

import com.training.marketplace.config.RecommendationProperties;
import com.training.marketplace.repository.SimilarProductRecommendationRepository;
import com.training.marketplace.service.RecommendationCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SimilarProductPrecomputeService {

    private final SimilarProductRecommendationRepository repository;
    private final RecommendationProperties properties;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<RecommendationCandidate> getOrCompute(Long sourceProductId, int limit) {
        repository.acquireSourceLock(sourceProductId);
        if (!repository.isComputed(sourceProductId)) {
            List<RecommendationCandidate> candidates = repository.computeTop(
                    sourceProductId,
                    properties.getSimilarCandidatePoolSize(),
                    properties.getSimilarPrecomputeSize());
            repository.replace(sourceProductId, candidates);
        }
        return repository.findTop(sourceProductId, limit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void invalidateAffected(Long productId) {
        repository.invalidateAffected(productId);
    }
}
