package com.training.marketplace.service;

import com.training.marketplace.dto.response.JourneyMergeResponse;

public interface AnonymousJourneyMergeService {

    JourneyMergeResponse merge(Long userId, String sessionId);
}
