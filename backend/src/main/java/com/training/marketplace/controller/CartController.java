package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.AddToCartRequest;
import com.training.marketplace.dto.request.UpdateCartItemRequest;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get current user's shopping cart")
    public ApiResponse<CartResponse> getCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        return ApiResponse.success(cartService.getCart(userId));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to shopping cart")
    public ApiResponse<CartResponse> addItem(
            Authentication authentication,
            @Valid @RequestBody AddToCartRequest request) {
        Long userId = getUserId(authentication);
        return ApiResponse.success("Item added to cart", cartService.addItem(userId, request));
    }

    @PutMapping("/items/{variantId}")
    @Operation(summary = "Update item quantity in shopping cart")
    public ApiResponse<CartResponse> updateQuantity(
            Authentication authentication,
            @PathVariable Long variantId,
            @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = getUserId(authentication);
        return ApiResponse.success("Cart updated", cartService.updateQuantity(userId, variantId, request.quantity()));
    }

    @DeleteMapping("/items/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove item from shopping cart")
    public void removeItem(
            Authentication authentication,
            @PathVariable Long variantId) {
        Long userId = getUserId(authentication);
        cartService.removeItem(userId, variantId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Clear shopping cart")
    public void clearCart(Authentication authentication) {
        Long userId = getUserId(authentication);
        cartService.clearCart(userId);
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
