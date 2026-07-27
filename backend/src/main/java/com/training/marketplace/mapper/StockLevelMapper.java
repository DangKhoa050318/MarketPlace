package com.training.marketplace.mapper;

import com.training.marketplace.dto.response.StockLevelResponse;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.entity.StockLevel;
import com.training.marketplace.entity.Warehouse;
import com.training.marketplace.repository.projection.StockLevelProjection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StockLevelMapper {

    @Mapping(target = "id", source = "stockLevel.id")
    @Mapping(target = "variantId", source = "stockLevel.variantId")
    @Mapping(target = "sku", source = "variant.sku")
    @Mapping(target = "variantName", source = "variant.variantName")
    @Mapping(target = "warehouseId", source = "stockLevel.warehouseId")
    @Mapping(target = "warehouseCode", source = "warehouse.code")
    @Mapping(target = "warehouseName", source = "warehouse.name")
    @Mapping(target = "quantity", source = "stockLevel.quantity")
    @Mapping(target = "reservedQuantity", source = "stockLevel.reservedQuantity")
    @Mapping(
            target = "availableQuantity",
            expression = "java(stockLevel.getQuantity() - stockLevel.getReservedQuantity())")
    @Mapping(target = "minStock", source = "variant.minStock")
    @Mapping(target = "maxStock", source = "variant.maxStock")
    @Mapping(target = "reorderPoint", source = "variant.reorderPoint")
    @Mapping(target = "version", source = "stockLevel.version")
    @Mapping(target = "updatedAt", source = "stockLevel.updatedAt")
    StockLevelResponse toResponse(
            StockLevel stockLevel,
            ProductVariant variant,
            Warehouse warehouse);

    StockLevelResponse toResponse(StockLevelProjection projection);
}
