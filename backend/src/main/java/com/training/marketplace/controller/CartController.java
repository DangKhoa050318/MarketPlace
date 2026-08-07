package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.AddToCartRequest;
import com.training.marketplace.dto.request.UpdateCartItemRequest;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.service.CartService;
import com.training.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Tag(name = "Cart", description = "Shopping Cart REST APIs")
public class CartController {

    private final CartService cartService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get current user's shopping cart")
    public ApiResponse<CartResponse> getCart(Authentication authentication) {
        Long userId = userService.getAuthenticatedUser(authentication).getId();
        return ApiResponse.success(cartService.getCart(userId));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to shopping cart")
    public ApiResponse<CartResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request) {
        Long userId = userService.getAuthenticatedUser(authentication).getId();
        return ApiResponse.success("Item added to cart", cartService.addItem(userId, request));
    }

    @PutMapping("/items/{variantId}")
    @Operation(summary = "Update item quantity in shopping cart")
    public ApiResponse<CartResponse> updateQuantity(
            Authentication authentication,
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = userService.getAuthenticatedUser(authentication).getId();
        return ApiResponse.success("Cart updated", cartService.updateQuantity(userId, variantId, request.quantity()));
    }

    @DeleteMapping("/items/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove item from shopping cart")
    public void removeItem(
            Authentication authentication,
            @PathVariable Long variantId) {
        Long userId = userService.getAuthenticatedUser(authentication).getId();
        cartService.removeItem(userId, variantId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear shopping cart")
    public void clearCart(Authentication authentication) {
        Long userId = userService.getAuthenticatedUser(authentication).getId();
        cartService.clearCart(userId);
    }
}

