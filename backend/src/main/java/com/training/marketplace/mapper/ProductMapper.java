package com.training.marketplace.mapper;

import com.training.marketplace.dto.request.CreateProductRequest;
import com.training.marketplace.dto.request.UpdateProductRequest;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.entity.Product;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProductMapper {

    @Mapping(target = "variants", ignore = true)   // populated by the service on detail
    ProductResponse toResponse(Product product);

    Product toEntity(CreateProductRequest request); // categoryId/name/slug/unit/imageUrl map directly

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)          // immutable business key
    @Mapping(target = "searchVector", ignore = true)  // DB trigger populates
    void updateEntity(@MappingTarget Product product, UpdateProductRequest request);
}
