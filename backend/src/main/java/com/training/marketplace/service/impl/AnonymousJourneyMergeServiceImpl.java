package com.training.marketplace.service.impl;

import com.training.marketplace.dto.response.JourneyMergeResponse;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.AnalyticsEventRepository;
import com.training.marketplace.repository.AnonymousWishlistItemRepository;
import com.training.marketplace.repository.RecentlyViewedProductRepository;
import com.training.marketplace.service.AnonymousJourneyMergeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnonymousJourneyMergeServiceImpl implements AnonymousJourneyMergeService {

    private final RecentlyViewedProductRepository recentlyViewedProductRepository;
    private final AnonymousWishlistItemRepository anonymousWishlistItemRepository;
    private final AnalyticsEventRepository analyticsEventRepository;

    @Override
    @Transactional
    public JourneyMergeResponse merge(Long userId, String sessionId) {
        if (userId == null) {
            throw new BadRequestException("Authenticated user is required for journey merge");
        }
        if (sessionId == null || sessionId.isBlank() || sessionId.length() > 128) {
            throw new BadRequestException("Valid anonymous session ID is required");
        }

        String normalizedSessionId = sessionId.trim();
        int wishlistMerged = anonymousWishlistItemRepository.mergeSessionIntoUser(normalizedSessionId, userId);
        anonymousWishlistItemRepository.deleteBySessionId(normalizedSessionId);
        int recentMerged = recentlyViewedProductRepository.mergeSessionIntoUser(normalizedSessionId, userId);
        recentlyViewedProductRepository.deleteBySessionId(normalizedSessionId);
        int eventsLinked = analyticsEventRepository.linkAnonymousSessionToUser(normalizedSessionId, userId);
        return new JourneyMergeResponse(wishlistMerged, recentMerged, eventsLinked);
    }
}