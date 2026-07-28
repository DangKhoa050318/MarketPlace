package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.AdminUpdateReviewStatusRequest;
import com.training.marketplace.dto.response.ProductReviewResponse;
import com.training.marketplace.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
@Tag(name = "Admin Review Moderation", description = "Admin & Manager endpoints for moderating customer reviews")
public class AdminReviewController {

    private final ReviewService reviewService;

    @PutMapping("/{id}/status")
    @Operation(summary = "Moderate review status", description = "Approve, hide, or soft delete a customer review")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> updateReviewStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateReviewStatusRequest request) {
        ProductReviewResponse response = reviewService.adminUpdateStatus(id, request.status());
        return ResponseEntity.ok(ApiResponse.success("Review status updated successfully", response));
    }
}
