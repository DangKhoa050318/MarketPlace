package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.config.OpenApiSchemas;
import com.training.marketplace.dto.request.AddDeliveryEventRequest;
import com.training.marketplace.dto.request.CreateDeliveryRequest;
import com.training.marketplace.dto.request.UpdateDeliveryRequest;
import com.training.marketplace.dto.request.UpdateDeliveryStatusRequest;
import com.training.marketplace.dto.response.DeliveryResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
@Tag(name = "Admin Delivery Tracking", description = "Back-office delivery management endpoints")
public class AdminDeliveryController {

    private final DeliveryService deliveryService;
    private final UserRepository userRepository;

    @PostMapping("/orders/{orderId}/delivery")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create delivery tracking for a shipped order")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Delivery created", content = @Content(schema = @Schema(implementation = OpenApiSchemas.DeliveryResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order is not shipped or request validation failed", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Back-office role required", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Delivery or tracking code already exists", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<DeliveryResponse> create(
            @PathVariable Long orderId,
            @Valid @RequestBody CreateDeliveryRequest request,
            Authentication authentication) {
        return ApiResponse.success("Delivery created", deliveryService.create(
                orderId, resolveUser(authentication).getId(), request));
    }

    @GetMapping("/orders/{orderId}/delivery")
    @Operation(summary = "Get delivery tracking for any order")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery returned", content = @Content(schema = @Schema(implementation = OpenApiSchemas.DeliveryResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Back-office role required", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order or delivery not found", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<DeliveryResponse> get(@PathVariable Long orderId) {
        return ApiResponse.success(deliveryService.getForAdmin(orderId));
    }

    @PatchMapping("/deliveries/{deliveryId}")
    @Operation(summary = "Update carrier, tracking code, or ETA while delivery is pending")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery updated", content = @Content(schema = @Schema(implementation = OpenApiSchemas.DeliveryResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Delivery is not pending or request validation failed", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Back-office role required", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery not found", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Tracking code already exists", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<DeliveryResponse> update(
            @PathVariable Long deliveryId,
            @Valid @RequestBody UpdateDeliveryRequest request) {
        return ApiResponse.success("Delivery updated", deliveryService.update(
                deliveryId, request));
    }

    @PatchMapping("/deliveries/{deliveryId}/status")
    @Operation(summary = "Transition delivery status and append a timeline event")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery status updated", content = @Content(schema = @Schema(implementation = OpenApiSchemas.DeliveryResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid delivery transition", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Back-office role required", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery not found", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<DeliveryResponse> updateStatus(
            @PathVariable Long deliveryId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request,
            Authentication authentication) {
        return ApiResponse.success("Delivery status updated", deliveryService.updateStatus(
                deliveryId, resolveUser(authentication).getId(), request));
    }

    @PostMapping("/deliveries/{deliveryId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Append an informational delivery milestone")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Milestone recorded", content = @Content(schema = @Schema(implementation = OpenApiSchemas.DeliveryResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Milestone is invalid for the current status", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Back-office role required", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery not found", content = @Content(schema = @Schema(implementation = OpenApiSchemas.ErrorResponseEnvelope.class)))
    })
    public ApiResponse<DeliveryResponse> addEvent(
            @PathVariable Long deliveryId,
            @Valid @RequestBody AddDeliveryEventRequest request,
            Authentication authentication) {
        return ApiResponse.success("Delivery event recorded", deliveryService.addEvent(
                deliveryId, resolveUser(authentication).getId(), request));
    }

    private User resolveUser(Authentication authentication) {
        String identifier = authentication.getName();
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmail(identifier))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + identifier));
    }
}
