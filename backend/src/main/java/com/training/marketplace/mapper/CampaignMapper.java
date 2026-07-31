package com.training.marketplace.mapper;

import com.training.marketplace.dto.request.CreateCampaignRequest;
import com.training.marketplace.dto.request.UpdateCampaignRequest;
import com.training.marketplace.dto.response.CampaignResponse;
import com.training.marketplace.entity.Campaign;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CampaignMapper {

    /** {@code couponCode} is resolved by the service from {@code promotionCodeId} (null when unset). */
    CampaignResponse toResponse(Campaign campaign, String couponCode);

    Campaign toEntity(CreateCampaignRequest request);

    void updateEntity(UpdateCampaignRequest request, @MappingTarget Campaign campaign);
}
