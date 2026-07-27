package com.training.marketplace.mapper;

import com.training.marketplace.dto.response.MovementItemResponse;
import com.training.marketplace.dto.response.MovementResponse;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.entity.StockMovement;
import com.training.marketplace.entity.StockMovementItem;
import com.training.marketplace.entity.Warehouse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface StockMovementMapper {

    @Mapping(target = "id", source = "movement.id")
    @Mapping(target = "referenceNo", source = "movement.referenceNo")
    @Mapping(target = "type", expression = "java(movement.getType().name())")
    @Mapping(target = "status", expression = "java(movement.getStatus().name())")
    @Mapping(target = "warehouseId", source = "movement.warehouseId")
    @Mapping(target = "warehouseCode", source = "warehouse.code")
    @Mapping(target = "warehouseName", source = "warehouse.name")
    @Mapping(target = "destWarehouseId", source = "movement.destWarehouseId")
    @Mapping(target = "destWarehouseCode", source = "destWarehouse.code")
    @Mapping(target = "destWarehouseName", source = "destWarehouse.name")
    @Mapping(target = "notes", source = "movement.notes")
    @Mapping(target = "createdBy", source = "movement.createdBy")
    @Mapping(target = "approvedBy", source = "movement.approvedBy")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "createdAt", source = "movement.createdAt")
    MovementResponse toResponse(
            StockMovement movement,
            Warehouse warehouse,
            Warehouse destWarehouse,
            List<MovementItemResponse> items);

    @Mapping(target = "id", source = "item.id")
    @Mapping(target = "variantId", source = "item.variantId")
    @Mapping(target = "sku", source = "variant.sku")
    @Mapping(target = "variantName", source = "variant.variantName")
    @Mapping(target = "quantity", source = "item.quantity")
    @Mapping(target = "unitCost", source = "item.unitCost")
    @Mapping(target = "batchNumber", source = "item.batchNumber")
    @Mapping(target = "expiryDate", source = "item.expiryDate")
    @Mapping(target = "notes", source = "item.notes")
    MovementItemResponse toItemResponse(StockMovementItem item, ProductVariant variant);
}
