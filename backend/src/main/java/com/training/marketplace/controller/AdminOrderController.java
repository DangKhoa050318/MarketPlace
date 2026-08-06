package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.AdminReturnDecisionRequest;
import com.training.marketplace.dto.request.PartialRefundRequest;
import com.training.marketplace.dto.request.ReturnQcRequest;
import com.training.marketplace.dto.request.UpdateOrderStatusRequest;
import com.training.marketplace.dto.response.OrderResponse;
import com.training.marketplace.dto.response.ReturnRequestResponse;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.service.OrderService;
import com.training.marketplace.service.ReturnRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
@Tag(name = "Admin Order Management", description = "Admin endpoints for querying and updating customer orders")
public class AdminOrderController {

    private final OrderService orderService;
    private final ReturnRequestService returnRequestService;

    @GetMapping
    @Operation(summary = "Get paginated orders list for Admin", description = "Retrieve all customer orders with optional status filtering")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Orders retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin role required")
    })
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAdminOrders(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        PageResponse<OrderResponse> response = orderService.getAdminOrders(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/count")
    @Operation(summary = "Count orders placed by a customer", description = "Total number of orders for a userId (reviewer trust signal)")
    public ResponseEntity<ApiResponse<Long>> countOrdersByUser(@RequestParam Long userId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.countOrdersByUser(userId)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update customer order status by Admin", description = "Update order status following validated state transition rules")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Order status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid status transition request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized access token missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden - Admin role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderResponse response = orderService.updateOrderStatusByAdmin(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated successfully", response));
    }

    @GetMapping("/return-requests")
    @Operation(summary = "Get open return requests")
    public ResponseEntity<ApiResponse<java.util.List<ReturnRequestResponse>>> getReturnRequests() {
        return ResponseEntity.ok(ApiResponse.success(returnRequestService.getOpenRequests()));
    }

    @PutMapping("/return-requests/{id}/decision")
    @Operation(summary = "Approve or reject a customer return request")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> decideReturn(
            @PathVariable Long id,
            @RequestBody AdminReturnDecisionRequest request) {
        ReturnRequestResponse response = returnRequestService.decide(id, request);
        return ResponseEntity.ok(ApiResponse.success("Return request decision saved", response));
    }

    @PutMapping("/return-requests/{id}/qc")
    @Operation(summary = "Record return QC result and refund when QC passes")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> recordReturnQc(
            @PathVariable Long id,
            @RequestBody ReturnQcRequest request) {
        ReturnRequestResponse response = returnRequestService.recordQc(id, request);
        return ResponseEntity.ok(ApiResponse.success("Return QC recorded", response));
    }

    @PostMapping("/{id}/refunds/partial")
    @Operation(summary = "Create a partial refund for an order item")
    public ResponseEntity<ApiResponse<ReturnRequestResponse>> partialRefund(
            @PathVariable Long id,
            @RequestBody PartialRefundRequest request) {
        ReturnRequestResponse response = returnRequestService.partialRefund(id, request);
        return ResponseEntity.ok(ApiResponse.success("Partial refund requested", response));
    }
}
