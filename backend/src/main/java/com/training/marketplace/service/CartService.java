package com.training.marketplace.service;

import com.training.marketplace.dto.request.AddToCartRequest;
import com.training.marketplace.dto.response.CartResponse;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, AddToCartRequest request);
    CartResponse updateQuantity(Long userId, Long variantId, int quantity);
    void removeItem(Long userId, Long variantId);
    void clearCart(Long userId);
}
