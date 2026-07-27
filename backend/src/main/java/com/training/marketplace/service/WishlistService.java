package com.training.marketplace.service;

import com.training.marketplace.dto.response.WishlistItemResponse;
import com.training.marketplace.dto.response.WishlistStatusResponse;

public interface WishlistService {

    WishlistItemResponse add(Long userId, Long productId);

    void remove(Long userId, Long productId);

    WishlistStatusResponse status(Long userId, Long productId);
}
