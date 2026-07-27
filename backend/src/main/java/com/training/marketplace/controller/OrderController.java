package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateOrderRequest;
import com.training.marketplace.dto.response.OrderResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Customer Orders", description = "Endpoints for placing orders and tracking customer order history")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Place a new order from current shopping cart")
    public ApiResponse<OrderResponse> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {
        Long userId = getUserId(authentication);
        OrderResponse response = orderService.createOrder(userId, request);
        return ApiResponse.success("Order placed successfully", response);
    }

    @GetMapping
    @Operation(summary = "Get order history for current authenticated user")
    public ApiResponse<PageResponse<OrderResponse>> getUserOrders(
            Authentication authentication,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(orderService.getUserOrders(userId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order detail by ID for current authenticated user")
    public ApiResponse<OrderResponse> getUserOrderById(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(orderService.getUserOrderById(userId, id));
    }

    @PutMapping("/{id}/cancel")
    @Operation(summary = "Cancel an order by current authenticated user")
    public ApiResponse<OrderResponse> cancelUserOrder(
            Authentication authentication,
            @PathVariable Long id) {
        Long userId = getUserId(authentication);
        return ApiResponse.success("Order cancelled successfully", orderService.cancelUserOrder(userId, id));
    }

    private Long getUserId(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new ResourceNotFoundException("Unauthenticated user context");
        }
        String identifier = auth.getName();
        User user = userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier));
        return user.getId();
    }
}
