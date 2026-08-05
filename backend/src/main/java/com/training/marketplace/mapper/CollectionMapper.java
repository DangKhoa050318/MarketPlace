package com.training.marketplace.mapper;

import com.training.marketplace.dto.request.CreateCollectionRequest;
import com.training.marketplace.dto.request.UpdateCollectionRequest;
import com.training.marketplace.dto.response.CollectionItemResponse;
import com.training.marketplace.dto.response.CollectionResponse;
import com.training.marketplace.entity.ProductCollection;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CollectionMapper {

    /** {@code items} are built by the service (product lookup for name/slug/image). */
    CollectionResponse toResponse(ProductCollection collection, List<CollectionItemResponse> items);

    ProductCollection toEntity(CreateCollectionRequest request);

    void updateEntity(UpdateCollectionRequest request, @MappingTarget ProductCollection collection);
}
