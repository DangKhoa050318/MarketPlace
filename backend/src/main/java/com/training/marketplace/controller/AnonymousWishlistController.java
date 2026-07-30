package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.response.WishlistStatusResponse;
import com.training.marketplace.service.AnonymousWishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/anonymous-wishlist")
@RequiredArgsConstructor
@Tag(name = "Anonymous Wishlist", description = "Session wishlist operations before login")
public class AnonymousWishlistController {

    private final AnonymousWishlistService anonymousWishlistService;

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a product to an anonymous session wishlist")
    public ApiResponse<WishlistStatusResponse> add(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable Long productId) {
        return ApiResponse.success("Product added to anonymous wishlist",
                anonymousWishlistService.add(sessionId, productId));
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a product from an anonymous session wishlist")
    public void remove(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable Long productId) {
        anonymousWishlistService.remove(sessionId, productId);
    }

    @GetMapping("/{productId}/status")
    @Operation(summary = "Check whether a product is in an anonymous session wishlist")
    public ApiResponse<WishlistStatusResponse> status(
            @RequestHeader("X-Session-Id") String sessionId,
            @PathVariable Long productId) {
        return ApiResponse.success(anonymousWishlistService.status(sessionId, productId));
    }
}
