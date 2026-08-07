package com.training.marketplace.controller;

import com.training.marketplace.analytics.RecommendationPlacement;
import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.response.RecommendationResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.service.RecommendationService;
import com.training.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
@RequiredArgsConstructor
@Tag(
        name = "Recommendations",
        description = "Storefront rule-based product recommendations")
public class RecommendationController {

    private static final int DEFAULT_LIMIT = 12;

    private final RecommendationService recommendationService;
    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "Get recommendations for a storefront placement",
            description = """
                    The server maps placement to a registered strategy and returns a requestId
                    that must be reused for recommendation impression and click events.
                    Product-detail placements require productId; CATEGORY_BEST_SELLERS requires
                    categoryId.
                    """)
    public ApiResponse<RecommendationResponse> recommend(
            Authentication authentication,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Parameter(schema = @Schema(implementation = RecommendationPlacement.class))
            @RequestParam(required = false) String placement,
            @Parameter(description = "Source product/SPU for product-detail placements")
            @RequestParam(required = false) Long productId,
            @Parameter(description = "Category for CATEGORY_BEST_SELLERS")
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        return ApiResponse.success(recommendationService.recommend(
                currentUserId(authentication),
                sessionId,
                parsePlacement(placement),
                productId,
                categoryId,
                limit));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        try {
            User user = userService.getAuthenticatedUser(authentication);
            return user != null ? user.getId() : null;
        } catch (ResourceNotFoundException e) {
            return null;
        }
    }

    private RecommendationPlacement parsePlacement(String placement) {
        if (placement == null || placement.isBlank()) {
            return null;
        }
        try {
            return RecommendationPlacement.valueOf(placement.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(
                    "Unsupported recommendation placement: " + placement);
        }
    }
}

