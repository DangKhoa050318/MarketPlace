package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateOrderRequest;
import com.training.marketplace.dto.request.UpdateOrderStatusRequest;
import com.training.marketplace.dto.response.DashboardStatsResponse;
import com.training.marketplace.dto.response.OrderResponse;
import com.training.marketplace.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {

    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable);

    OrderResponse getUserOrderById(Long userId, Long orderId);

    OrderResponse cancelUserOrder(Long userId, Long orderId);

    PageResponse<OrderResponse> getAdminOrders(OrderStatus status, Pageable pageable);

    OrderResponse updateOrderStatusByAdmin(Long orderId, UpdateOrderStatusRequest request);

    DashboardStatsResponse getDashboardStats();
}
