package com.training.marketplace.mapper;

import com.training.marketplace.dto.request.CreateUserRequest;
import com.training.marketplace.dto.request.UpdateUserRequest;
import com.training.marketplace.dto.response.UserResponse;
import com.training.marketplace.entity.User;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().name())")
    UserResponse toResponse(User user);

    @Mapping(target = "role", ignore = true)
    @Mapping(target = "active", ignore = true)
    User toEntity(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateEntity(@MappingTarget User user, UpdateUserRequest request);
}
