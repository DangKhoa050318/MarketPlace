package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.CreateCampaignRequest;
import com.training.marketplace.dto.response.CampaignResponse;
import com.training.marketplace.entity.Campaign;
import com.training.marketplace.enums.CampaignStatus;
import com.training.marketplace.enums.MerchandisingTargetType;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ConflictException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.CampaignMapper;
import com.training.marketplace.repository.CampaignRepository;
import com.training.marketplace.repository.MerchandisingEventRepository;
import com.training.marketplace.repository.PromotionCodeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/** Unit tests for campaign window validation (T-401), promotion linking and status transitions. */
@ExtendWith(MockitoExtension.class)
class CampaignServiceImplTest {

    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private PromotionCodeRepository promotionCodeRepository;
    @Mock
    private MerchandisingEventRepository eventRepository;
    @Mock
    private CampaignMapper campaignMapper;

    @InjectMocks
    private CampaignServiceImpl service;

    private CampaignResponse dummyResponse() {
        return new CampaignResponse(1L, "Sale", null, CampaignStatus.DRAFT,
                null, null, null, null, true, null, null);
    }

    @Test
    void create_rejectsEndBeforeStart() {
        CreateCampaignRequest request = new CreateCampaignRequest(
                "Sale", null,
                LocalDateTime.of(2026, 8, 10, 0, 0),
                LocalDateTime.of(2026, 8, 1, 0, 0),
                null);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(campaignMapper);
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void create_rejectsUnknownPromotion() {
        CreateCampaignRequest request = new CreateCampaignRequest("Sale", null, null, null, 99L);
        given(promotionCodeRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BadRequestException.class);

        verify(campaignRepository, never()).save(any());
    }

    @Test
    void create_persistsDraft_whenValid() {
        CreateCampaignRequest request = new CreateCampaignRequest("Sale", "desc", null, null, null);
        Campaign entity = Campaign.builder().name("Sale").build();
        given(campaignMapper.toEntity(request)).willReturn(entity);
        given(campaignRepository.save(entity)).willReturn(entity);
        given(campaignMapper.toResponse(any(), any())).willReturn(dummyResponse());

        service.create(request);

        verify(campaignRepository).save(entity);
    }

    @Test
    void publish_setsPublishedStatus() {
        Campaign campaign = Campaign.builder().name("Sale").status(CampaignStatus.DRAFT).build();
        given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));
        given(campaignRepository.save(campaign)).willReturn(campaign);
        given(campaignMapper.toResponse(any(), any())).willReturn(dummyResponse());

        service.publish(1L);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.PUBLISHED);
        verify(campaignRepository).save(campaign);
    }

    @Test
    void archive_setsArchivedStatus() {
        Campaign campaign = Campaign.builder().name("Sale").status(CampaignStatus.PUBLISHED).build();
        given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));
        given(campaignRepository.save(campaign)).willReturn(campaign);
        given(campaignMapper.toResponse(any(), any())).willReturn(dummyResponse());

        service.archive(1L);

        assertThat(campaign.getStatus()).isEqualTo(CampaignStatus.ARCHIVED);
    }

    @Test
    void delete_softDeactivates() {
        Campaign campaign = Campaign.builder().name("Sale").active(true).build();
        given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));

        service.delete(1L);

        assertThat(campaign.getActive()).isFalse();
        verify(campaignRepository).save(campaign);
    }

    @Test
    void hardDelete_rejectsWhenLinkedToCoupon() {
        Campaign campaign = Campaign.builder().name("Sale").promotionCodeId(5L).build();
        given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));

        assertThatThrownBy(() -> service.hardDelete(1L))
                .isInstanceOf(ConflictException.class);

        verify(campaignRepository, never()).delete(any());
    }

    @Test
    void hardDelete_rejectsWhenEventHistoryExists() {
        Campaign campaign = Campaign.builder().name("Sale").build(); // no coupon link
        given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));
        given(eventRepository.existsByTargetTypeAndTargetId(MerchandisingTargetType.CAMPAIGN, 1L)).willReturn(true);

        assertThatThrownBy(() -> service.hardDelete(1L))
                .isInstanceOf(ConflictException.class);

        verify(campaignRepository, never()).delete(any());
    }

    @Test
    void hardDelete_deletesWhenStandalone() {
        Campaign campaign = Campaign.builder().name("Sale").build();
        given(campaignRepository.findById(1L)).willReturn(Optional.of(campaign));
        given(eventRepository.existsByTargetTypeAndTargetId(MerchandisingTargetType.CAMPAIGN, 1L)).willReturn(false);

        service.hardDelete(1L);

        verify(campaignRepository).delete(campaign);
    }

    @Test
    void getById_throwsWhenMissing() {
        given(campaignRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(2L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
