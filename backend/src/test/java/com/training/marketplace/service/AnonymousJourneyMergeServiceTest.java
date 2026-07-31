package com.training.marketplace.service;

import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.repository.AnonymousWishlistItemRepository;
import com.training.marketplace.repository.RecentlyViewedProductRepository;
import com.training.marketplace.service.impl.AnonymousJourneyMergeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnonymousJourneyMergeServiceTest {

    @Mock private RecentlyViewedProductRepository recentlyViewedProductRepository;
    @Mock private AnonymousWishlistItemRepository anonymousWishlistItemRepository;
    @Mock private AnalyticsEventRepository analyticsEventRepository;

    @InjectMocks private AnonymousJourneyMergeServiceImpl service;

    @Test
    void merge_movesWishlistRecentlyViewedAndLinksAnalyticsEvents() {
        when(anonymousWishlistItemRepository.mergeSessionIntoUser("session-1", 7L)).thenReturn(2);
        when(recentlyViewedProductRepository.mergeSessionIntoUser("session-1", 7L)).thenReturn(4);
        when(analyticsEventRepository.linkAnonymousSessionToUser("session-1", 7L)).thenReturn(9);

        var response = service.merge(7L, "session-1");

        assertThat(response.wishlistItemsMerged()).isEqualTo(2);
        assertThat(response.recentlyViewedItemsMerged()).isEqualTo(4);
        assertThat(response.analyticsEventsLinked()).isEqualTo(9);
        verify(anonymousWishlistItemRepository).deleteBySessionId("session-1");
        verify(recentlyViewedProductRepository).deleteBySessionId("session-1");
    }
    @Test
    void merge_requiresAuthenticatedUserAndValidSession() {
        assertThatThrownBy(() -> service.merge(null, "session-1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Authenticated user");
        assertThatThrownBy(() -> service.merge(7L, " "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("session ID");

        verifyNoInteractions(recentlyViewedProductRepository, analyticsEventRepository);
    }
}
