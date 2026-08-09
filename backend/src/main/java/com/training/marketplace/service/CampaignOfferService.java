package com.training.marketplace.service;

import java.util.List;

public interface CampaignOfferService {
    List<ChatCampaignOffer> findEffectiveOffers(Long userId);
}
