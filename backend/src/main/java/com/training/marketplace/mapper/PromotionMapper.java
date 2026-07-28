package com.training.marketplace.mapper;

import com.training.marketplace.dto.request.ScopeRefDto;
import com.training.marketplace.dto.response.PromotionCodeResponse;
import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.entity.PromotionCodeScope;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PromotionMapper {

    @Mapping(target = "scopes", source = "scopeEntities")
    PromotionCodeResponse toResponse(PromotionCode promo, List<PromotionCodeScope> scopeEntities);

    ScopeRefDto toScopeRef(PromotionCodeScope scope);
}
