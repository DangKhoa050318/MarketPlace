package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateOrderRequest;
import com.training.marketplace.dto.request.UpdateOrderStatusRequest;
import com.training.marketplace.dto.response.DashboardStatsResponse;
import com.training.marketplace.dto.response.OrderResponse;
import com.training.marketplace.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

import com.training.marketplace.dto.response.PaygatePayloadResponse;

import com.training.marketplace.entity.Order;

public interface OrderService {
    
    OrderResponse enrichOrderResponse(Order order);

    OrderResponse createOrder(Long userId, CreateOrderRequest request);



    PageResponse<OrderResponse> getUserOrders(Long userId, Pageable pageable);

    PageResponse<OrderResponse> getUserOrders(Long userId, OrderStatus status, com.training.marketplace.enums.PaymentStatus paymentStatus, String search, Pageable pageable);

    OrderResponse getUserOrderById(Long userId, Long orderId);

    OrderResponse cancelUserOrder(Long userId, Long orderId);


    OrderResponse confirmReceived(Long userId, Long orderId);

    PageResponse<OrderResponse> getAdminOrders(OrderStatus status, Pageable pageable);

    long countOrdersByUser(Long userId);

    OrderResponse updateOrderStatusByAdmin(Long orderId, UpdateOrderStatusRequest request);

    DashboardStatsResponse getDashboardStats();
}
