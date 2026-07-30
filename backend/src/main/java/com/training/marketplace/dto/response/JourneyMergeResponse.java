package com.training.marketplace.dto.response;

public record JourneyMergeResponse(
        int wishlistItemsMerged,
        int recentlyViewedItemsMerged,
        int analyticsEventsLinked
) {
}
