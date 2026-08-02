package com.training.marketplace.repository;

import com.training.marketplace.analytics.AnalyticsCacheNames;
import com.training.marketplace.dto.request.AnalyticsDashboardFilter;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.Cacheable;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyticsDashboardCachingTest {

    @Test
    void heavyAnalyticsQueriesUseDedicatedSynchronizedCaches() throws Exception {
        assertCache("overview", new Class<?>[]{AnalyticsDashboardFilter.class},
                AnalyticsCacheNames.OVERVIEW);
        assertCache("productPerformance",
                new Class<?>[]{AnalyticsDashboardFilter.class, int.class, int.class},
                AnalyticsCacheNames.PRODUCT_PERFORMANCE);
        assertCache("promotionRecommendationPerformance",
                new Class<?>[]{AnalyticsDashboardFilter.class},
                AnalyticsCacheNames.PROMOTION_RECOMMENDATION);
    }

    private void assertCache(String methodName, Class<?>[] parameterTypes, String expectedName)
            throws NoSuchMethodException {
        Cacheable cacheable = AnalyticsDashboardQueryRepository.class
                .getMethod(methodName, parameterTypes)
                .getAnnotation(Cacheable.class);

        assertThat(cacheable).isNotNull();
        assertThat(cacheable.cacheNames()).containsExactly(expectedName);
        assertThat(cacheable.sync()).isTrue();
    }
}
