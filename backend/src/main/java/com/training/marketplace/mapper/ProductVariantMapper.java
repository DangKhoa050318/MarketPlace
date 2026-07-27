package com.training.marketplace.mapper;

import com.training.marketplace.dto.request.CreateProductVariantRequest;
import com.training.marketplace.dto.request.UpdateProductVariantRequest;
import com.training.marketplace.dto.response.ProductVariantResponse;
import com.training.marketplace.entity.ProductVariant;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductVariantMapper {

    ProductVariantResponse toResponse(ProductVariant variant);

    List<ProductVariantResponse> toResponseList(List<ProductVariant> variants);

    @Mapping(target = "productId", ignore = true)   // set from path in the service
    ProductVariant toEntity(CreateProductVariantRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "sku", ignore = true)      // immutable business key
    @Mapping(target = "version", ignore = true)
    void updateEntity(@MappingTarget ProductVariant variant, UpdateProductVariantRequest request);
}
