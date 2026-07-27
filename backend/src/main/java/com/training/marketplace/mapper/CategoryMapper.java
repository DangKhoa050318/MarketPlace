package com.training.marketplace.mapper;

import com.training.marketplace.dto.request.CreateCategoryRequest;
import com.training.marketplace.dto.request.UpdateCategoryRequest;
import com.training.marketplace.dto.response.CategoryResponse;
import com.training.marketplace.entity.Category;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    CategoryResponse toResponse(Category category);

    Category toEntity(CreateCategoryRequest request); // name/code/slug/parentId map directly

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)   // immutable business key
    void updateEntity(@MappingTarget Category category, UpdateCategoryRequest request);
}
