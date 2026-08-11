package com.training.marketplace.service.impl;

import com.training.marketplace.config.RecommendationProperties;
import com.training.marketplace.service.ProductCatalogChangedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SimilarProductCatalogChangeListener {

    private final SimilarProductPrecomputeService precomputeService;
    private final RecommendationProperties properties;
    private final CacheManager cacheManager;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void refresh(ProductCatalogChangedEvent event) {
        precomputeService.invalidateAffected(event.productId());
        Cache cache = cacheManager.getCache(SimilarProductRecommendationStrategy.SIMILAR_CACHE);
        if (cache != null) {
            cache.clear();
        }
        precomputeService.getOrCompute(
                event.productId(),
                properties.getSimilarPrecomputeSize());
    }
}
