package com.training.marketplace.mapper;

import com.training.marketplace.dto.response.OrderItemResponse;
import com.training.marketplace.dto.response.OrderResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "userEmail", source = "user.email")
    @Mapping(target = "paygatePayload", ignore = true)
    OrderResponse toResponse(Order order);

    OrderItemResponse toItemResponse(OrderItem orderItem);
}
