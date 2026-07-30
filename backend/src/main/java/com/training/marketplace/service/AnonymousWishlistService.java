package com.training.marketplace.service;

import com.training.marketplace.dto.response.WishlistStatusResponse;

public interface AnonymousWishlistService {

    WishlistStatusResponse add(String sessionId, Long productId);

    void remove(String sessionId, Long productId);

    WishlistStatusResponse status(String sessionId, Long productId);
}
