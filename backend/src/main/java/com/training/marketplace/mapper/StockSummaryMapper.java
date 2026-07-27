package com.training.marketplace.mapper;

import com.training.marketplace.dto.response.StockSummaryResponse;
import com.training.marketplace.repository.projection.StockSummaryProjection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface StockSummaryMapper {

    StockSummaryResponse toResponse(StockSummaryProjection projection);
}
