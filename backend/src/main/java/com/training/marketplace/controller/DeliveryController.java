package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.config.OpenApiSchemas;
import com.training.marketplace.dto.response.DeliveryResponse;
import com.training.marketplace.service.DeliveryService;
import com.training.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Delivery Tracking", description = "Customer delivery tracking endpoints")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final UserService userService;

    @GetMapping("/{orderId}/delivery")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get delivery timeline for an order owned by the current customer")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery returned", content = @Content(schema = @Schema(implementation = OpenApiSchemas.DeliveryResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid authentication", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Customer role required", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order or delivery not found", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<DeliveryResponse> getDelivery(
            @PathVariable Long orderId, Authentication authentication) {
        Long userId = userService.getAuthenticatedUser(authentication).getId();
        return ApiResponse.success(deliveryService.getForCustomer(orderId, userId));
    }
}

