package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.response.WishlistItemResponse;
import com.training.marketplace.dto.response.WishlistStatusResponse;

public interface WishlistService {

    WishlistItemResponse add(Long userId, Long productId);

    PageResponse<WishlistItemResponse> list(Long userId, int page, int size);

    void remove(Long userId, Long productId);

    WishlistStatusResponse status(Long userId, Long productId);
}
