package com.training.marketplace.service.impl;

import com.training.marketplace.config.RecommendationProperties;
import com.training.marketplace.repository.SimilarProductRecommendationRepository;
import com.training.marketplace.service.RecommendationCandidate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimilarProductPrecomputeServiceTest {

    @Mock
    private SimilarProductRecommendationRepository repository;

    private SimilarProductPrecomputeService service;

    @BeforeEach
    void setUp() {
        RecommendationProperties properties = new RecommendationProperties();
        properties.setSimilarCandidatePoolSize(500);
        properties.setSimilarPrecomputeSize(50);
        service = new SimilarProductPrecomputeService(repository, properties);
    }

    @Test
    void getOrCompute_readsStoredTopKWhenSourceIsCurrent() {
        List<RecommendationCandidate> stored = List.of(
                new RecommendationCandidate(2L, 0.8, "same_category"));
        when(repository.isComputed(1L)).thenReturn(true);
        when(repository.findTop(1L, 12)).thenReturn(stored);

        assertThat(service.getOrCompute(1L, 12)).isEqualTo(stored);

        verify(repository).acquireSourceLock(1L);
        verify(repository, never()).computeTop(1L, 500, 50);
        verify(repository, never()).replace(1L, stored);
    }

    @Test
    void getOrCompute_computesAndStoresOnlyOnColdSource() {
        List<RecommendationCandidate> computed = List.of(
                new RecommendationCandidate(2L, 0.8, "same_category"));
        when(repository.isComputed(1L)).thenReturn(false);
        when(repository.computeTop(1L, 500, 50)).thenReturn(computed);
        when(repository.findTop(1L, 12)).thenReturn(computed);

        assertThat(service.getOrCompute(1L, 12)).isEqualTo(computed);

        verify(repository).replace(1L, computed);
    }
}
