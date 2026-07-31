package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.response.WishlistItemResponse;
import com.training.marketplace.dto.response.WishlistStatusResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.WishlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
@RequiredArgsConstructor
@Tag(name = "Wishlist", description = "User wishlist operations")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "List the current user's wishlist")
    public ApiResponse<PageResponse<WishlistItemResponse>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ApiResponse.success(wishlistService.list(currentUserId(authentication), page, size));
    }

    @PostMapping("/{productId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Add a product to the current user's wishlist")
    public ApiResponse<WishlistItemResponse> add(Authentication authentication, @PathVariable Long productId) {
        return ApiResponse.success("Product added to wishlist",
                wishlistService.add(currentUserId(authentication), productId));
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a product from the current user's wishlist")
    public void remove(Authentication authentication, @PathVariable Long productId) {
        wishlistService.remove(currentUserId(authentication), productId);
    }

    @GetMapping("/{productId}/status")
    @Operation(summary = "Check whether a product is in the current user's wishlist")
    public ApiResponse<WishlistStatusResponse> status(Authentication authentication, @PathVariable Long productId) {
        return ApiResponse.success(wishlistService.status(currentUserId(authentication), productId));
    }

    private Long currentUserId(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .map(User::getId)
                .orElseThrow(() -> new BadRequestException("Authenticated user could not be resolved"));
    }
}
