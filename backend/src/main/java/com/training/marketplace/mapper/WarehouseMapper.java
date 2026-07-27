package com.training.marketplace.mapper;

import com.training.marketplace.dto.request.CreateWarehouseRequest;
import com.training.marketplace.dto.request.UpdateWarehouseRequest;
import com.training.marketplace.dto.response.WarehouseResponse;
import com.training.marketplace.entity.Warehouse;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    WarehouseResponse toResponse(Warehouse warehouse);

    @Mapping(target = "active", ignore = true)
    Warehouse toEntity(CreateWarehouseRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    void updateEntity(@MappingTarget Warehouse warehouse, UpdateWarehouseRequest request);
}