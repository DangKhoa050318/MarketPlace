package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.RecordRecentlyViewedRequest;
import com.training.marketplace.dto.response.RecentlyViewedProductResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.service.RecentlyViewedProductService;
import com.training.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recently-viewed")
@RequiredArgsConstructor
@Tag(name = "Recently Viewed", description = "User or anonymous session recently viewed products")
public class RecentlyViewedProductController {

    private static final int DEFAULT_LIMIT = 20;

    private final RecentlyViewedProductService recentlyViewedService;
    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Record a recently viewed product for the current user or session")
    public ApiResponse<RecentlyViewedProductResponse> record(
            Authentication authentication,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody RecordRecentlyViewedRequest request) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success("Recently viewed product recorded",
                recentlyViewedService.record(userId, sessionId, request.productId()));
    }

    @GetMapping
    @Operation(summary = "List recently viewed products, newest first")
    public ApiResponse<List<RecentlyViewedProductResponse>> list(
            Authentication authentication,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        Long userId = currentUserId(authentication);
        return ApiResponse.success(recentlyViewedService.list(userId, sessionId, limit));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear recently viewed products for the current user or session")
    public void clear(
            Authentication authentication,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        Long userId = currentUserId(authentication);
        recentlyViewedService.clear(userId, sessionId);
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
}

