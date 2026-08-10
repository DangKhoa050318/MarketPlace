package com.training.marketplace.service.impl;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateCampaignRequest;
import com.training.marketplace.dto.request.UpdateCampaignRequest;
import com.training.marketplace.dto.response.CampaignResponse;
import com.training.marketplace.entity.Campaign;
import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.enums.CampaignStatus;
import com.training.marketplace.enums.MerchandisingTargetType;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ConflictException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.CampaignMapper;
import com.training.marketplace.repository.CampaignRepository;
import com.training.marketplace.repository.MerchandisingEventRepository;
import com.training.marketplace.repository.PromotionCodeRepository;
import com.training.marketplace.service.CampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignServiceImpl implements CampaignService {

    private final CampaignRepository campaignRepository;
    private final PromotionCodeRepository promotionCodeRepository;
    private final MerchandisingEventRepository eventRepository;
    private final CampaignMapper campaignMapper;

    @Override
    @Transactional
    public CampaignResponse create(CreateCampaignRequest request) {
        validateWindow(request.startsAt(), request.endsAt());
        validatePromotion(request.promotionCodeId());

        Campaign campaign = campaignMapper.toEntity(request); // status=DRAFT, active=true (entity defaults)
        Campaign saved = campaignRepository.save(campaign);
        log.info("Campaign created: id={}, name={}", saved.getId(), saved.getName());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public CampaignResponse update(Long id, UpdateCampaignRequest request) {
        Campaign campaign = findOr404(id);
        validateWindow(request.startsAt(), request.endsAt());
        validatePromotion(request.promotionCodeId());
        campaignMapper.updateEntity(request, campaign);
        return toResponse(campaignRepository.save(campaign));
    }

    @Override
    @Transactional(readOnly = true)
    public CampaignResponse getById(Long id) {
        return toResponse(findOr404(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CampaignResponse> list(CampaignStatus status, String q, Pageable pageable) {
        return PageResponse.from(campaignRepository.search(status, q, pageable), this::toResponse);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Campaign campaign = findOr404(id);
        campaign.setActive(false);
        campaignRepository.save(campaign);
        log.info("Campaign soft-deleted: id={}", id);
    }

    @Override
    @Transactional
    public CampaignResponse restore(Long id) {
        Campaign campaign = findOr404(id);
        campaign.setActive(true);
        log.info("Campaign restored (active=true): id={}", id);
        return toResponse(campaignRepository.save(campaign));
    }

    @Override
    @Transactional
    public void hardDelete(Long id) {
        Campaign campaign = findOr404(id);
        if (campaign.getPromotionCodeId() != null) {
            throw new ConflictException(
                    "Campaign is linked to a coupon and can only be deactivated, not permanently deleted");
        }
        if (eventRepository.existsByTargetTypeAndTargetId(MerchandisingTargetType.CAMPAIGN, id)) {
            throw new ConflictException(
                    "Campaign has recorded impression/click history and can only be deactivated, not permanently deleted");
        }
        campaignRepository.delete(campaign);
        log.info("Campaign hard-deleted: id={}", id);
    }

    @Override
    @Transactional
    public CampaignResponse publish(Long id) {
        Campaign campaign = findOr404(id);
        campaign.setStatus(CampaignStatus.PUBLISHED);
        return toResponse(campaignRepository.save(campaign));
    }

    @Override
    @Transactional
    public CampaignResponse archive(Long id) {
        Campaign campaign = findOr404(id);
        campaign.setStatus(CampaignStatus.ARCHIVED);
        return toResponse(campaignRepository.save(campaign));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CampaignResponse> listEffective() {
        return campaignRepository.findEffective(LocalDateTime.now()).stream().map(this::toResponse).toList();
    }

    // ---------------------------------------------------------------- helpers

    private Campaign findOr404(Long id) {
        return campaignRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", id));
    }

    private void validateWindow(LocalDateTime startsAt, LocalDateTime endsAt) {
        if (startsAt != null && endsAt != null && endsAt.isBefore(startsAt)) {
            throw new BadRequestException("endsAt must not be before startsAt");
        }
    }

    private void validatePromotion(Long promotionCodeId) {
        if (promotionCodeId != null && !promotionCodeRepository.existsById(promotionCodeId)) {
            throw new BadRequestException("Promotion code not found with id: " + promotionCodeId);
        }
    }

    private CampaignResponse toResponse(Campaign campaign) {
        return campaignMapper.toResponse(campaign, resolveCouponCode(campaign.getPromotionCodeId()));
    }

    private String resolveCouponCode(Long promotionCodeId) {
        if (promotionCodeId == null) {
            return null;
        }
        return promotionCodeRepository.findById(promotionCodeId)
                .map(PromotionCode::getCode)
                .orElse(null);
    }
}
