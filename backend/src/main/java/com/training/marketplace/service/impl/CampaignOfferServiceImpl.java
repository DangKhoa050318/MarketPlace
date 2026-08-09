package com.training.marketplace.service.impl;

import com.training.marketplace.repository.ChatCampaignOfferRepository;
import com.training.marketplace.service.CampaignOfferService;
import com.training.marketplace.service.ChatCampaignOffer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CampaignOfferServiceImpl implements CampaignOfferService {

    private final ChatCampaignOfferRepository offerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ChatCampaignOffer> findEffectiveOffers(Long userId) {
        return offerRepository.findEffective(userId, LocalDateTime.now());
    }
}
