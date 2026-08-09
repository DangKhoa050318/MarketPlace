package com.training.marketplace.service;

import java.util.List;

public interface ChatProductDiscoveryService {
    List<ChatProductCandidate> discover(
            ChatSearchCriteria criteria,
            boolean bestSeller,
            Long userId,
            String sessionId);
}
