package com.training.marketplace.mapper;

import com.training.marketplace.dto.request.CreateBannerRequest;
import com.training.marketplace.dto.request.UpdateBannerRequest;
import com.training.marketplace.dto.response.BannerResponse;
import com.training.marketplace.entity.MerchandisingBanner;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BannerMapper {

    BannerResponse toResponse(MerchandisingBanner banner);

    MerchandisingBanner toEntity(CreateBannerRequest request);

    void updateEntity(UpdateBannerRequest request, @MappingTarget MerchandisingBanner banner);
}
