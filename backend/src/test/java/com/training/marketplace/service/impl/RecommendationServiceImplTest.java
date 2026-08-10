package com.training.marketplace.service.impl;

import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.mapper.ProductMapper;
import com.training.marketplace.mapper.ProductVariantMapper;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.service.RecommendationStrategyRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Unit tests for recommendation request validation (MP-L4). These guard the branchy input rules that
 * decide whether a request is even dispatched to a strategy — every case here must fail fast, before
 * any repository/strategy is touched.
 */
@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private RecommendationStrategyRegistry strategyRegistry;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductVariantMapper variantMapper;

    @InjectMocks
    private RecommendationServiceImpl service;

    @Test
    void nullPlacement_throwsAndDispatchesNothing() {
        assertThatThrownBy(() -> service.recommend(1L, null, null, null, null, 5))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(strategyRegistry, productRepository);
    }

    @Test
    void limitBelowOne_throws() {
        assertThatThrownBy(() -> service.recommend(1L, null, RecommendationPlacement.HOME_BEST_SELLERS, null, null, 0))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(strategyRegistry);
    }

    @Test
    void limitAboveMax_throws() {
        assertThatThrownBy(() -> service.recommend(1L, null, RecommendationPlacement.HOME_BEST_SELLERS, null, null,
                RecommendationServiceImpl.MAX_LIMIT + 1))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(strategyRegistry);
    }

    @Test
    void productDetailPlacement_withoutProductId_throws() {
        assertThatThrownBy(() -> service.recommend(1L, null,
                RecommendationPlacement.PRODUCT_DETAIL_SIMILAR, null, null, 5))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(strategyRegistry);
    }

    @Test
    void productDetailPlacement_withNonPositiveProductId_throws() {
        assertThatThrownBy(() -> service.recommend(1L, null,
                RecommendationPlacement.PRODUCT_DETAIL_SIMILAR, -1L, null, 5))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(strategyRegistry);
    }

    @Test
    void categoryBestSellers_withoutCategoryId_throws() {
        assertThatThrownBy(() -> service.recommend(1L, null,
                RecommendationPlacement.CATEGORY_BEST_SELLERS, null, null, 5))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(strategyRegistry);
    }

    @Test
    void sessionIdExceedingMaxLength_throws() {
        String tooLong = "s".repeat(RecommendationServiceImpl.MAX_SESSION_ID_LENGTH + 1);
        assertThatThrownBy(() -> service.recommend(1L, tooLong,
                RecommendationPlacement.HOME_BEST_SELLERS, null, null, 5))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(strategyRegistry);
    }
}
