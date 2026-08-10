package com.training.marketplace.service.impl;

import com.training.marketplace.entity.Category;
import com.training.marketplace.repository.CategoryRepository;
import com.training.marketplace.repository.ChatPreferenceQueryRepository;
import com.training.marketplace.repository.ChatProductQueryRepository;
import com.training.marketplace.service.ChatPreferenceProfile;
import com.training.marketplace.service.ChatProductCandidate;
import com.training.marketplace.service.ChatSearchCriteria;
import com.training.marketplace.service.RecommendationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatProductDiscoveryServiceImplTest {

    @Mock private ChatProductQueryRepository productQueryRepository;
    @Mock private ChatPreferenceQueryRepository preferenceQueryRepository;
    @Mock private RecommendationService recommendationService;
    @Mock private CategoryRepository categoryRepository;

    @Test
    void retriesByDetectedCategoryWhenSoftNeedWordsHaveNoExactMatch() {
        var service = new ChatProductDiscoveryServiceImpl(
                productQueryRepository,
                preferenceQueryRepository,
                recommendationService,
                categoryRepository);
        Category laptops = Category.builder()
                .name("Computers & Laptops")
                .code("COMP")
                .slug("computers-laptops")
                .build();
        laptops.setId(2L);
        ChatProductCandidate laptop = candidate();
        when(preferenceQueryRepository.load(null, "session-1"))
                .thenReturn(ChatPreferenceProfile.empty());
        when(categoryRepository.findAll()).thenReturn(List.of(laptops));
        when(productQueryRepository.search(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(laptop));

        var result = service.discover(new ChatSearchCriteria(
                "laptop de hoc tap pin tot",
                null,
                null,
                null,
                new BigDecimal("20000000"),
                null,
                Set.of(),
                Set.of(),
                false,
                false,
                6), false, null, "session-1");

        assertThat(result).containsExactly(laptop);
        ArgumentCaptor<ChatSearchCriteria> criteriaCaptor =
                ArgumentCaptor.forClass(ChatSearchCriteria.class);
        verify(productQueryRepository, org.mockito.Mockito.times(2))
                .search(criteriaCaptor.capture());
        ChatSearchCriteria relaxed = criteriaCaptor.getAllValues().get(1);
        assertThat(relaxed.query()).isNull();
        assertThat(relaxed.pageCategoryId()).isEqualTo(2L);
        assertThat(relaxed.maxPrice()).isEqualByComparingTo(new BigDecimal("20000000"));
    }

    private ChatProductCandidate candidate() {
        return new ChatProductCandidate(
                10L,
                "laptop-pro",
                "Laptop Pro",
                "Long battery life",
                2L,
                "Computers & Laptops",
                "MarketBrand",
                null,
                101L,
                "16GB / 512GB",
                new BigDecimal("18000000"),
                new BigDecimal("18000000"),
                new BigDecimal("19500000"),
                8);
    }
}
