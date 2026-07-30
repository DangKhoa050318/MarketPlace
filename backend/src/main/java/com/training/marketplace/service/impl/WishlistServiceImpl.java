package com.training.marketplace.service.impl;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.dto.response.WishlistItemResponse;
import com.training.marketplace.dto.response.WishlistStatusResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.WishlistItem;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.ProductMapper;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.WishlistItemRepository;
import com.training.marketplace.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistServiceImpl implements WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public WishlistItemResponse add(Long userId, Long productId) {
        Product product = findActiveProduct(productId);
        WishlistItem item = wishlistItemRepository.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> wishlistItemRepository.save(WishlistItem.builder()
                        .userId(userId)
                        .productId(productId)
                        .build()));
        return toResponse(item, product);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WishlistItemResponse> list(Long userId, int page, int size) {
        int boundedPage = Math.max(0, page);
        int boundedSize = Math.max(1, Math.min(size, 50));
        var pageable = PageRequest.of(
                boundedPage,
                boundedSize,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return PageResponse.from(
                wishlistItemRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId, pageable),
                item -> toResponse(item, findActiveProduct(item.getProductId())));
    }

    @Override
    @Transactional
    public void remove(Long userId, Long productId) {
        wishlistItemRepository.deleteByUserIdAndProductId(userId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistStatusResponse status(Long userId, Long productId) {
        boolean wishlisted = wishlistItemRepository.existsByUserIdAndProductId(userId, productId);
        return new WishlistStatusResponse(productId, wishlisted);
    }

    private Product findActiveProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }

    private WishlistItemResponse toResponse(WishlistItem item, Product product) {
        ProductResponse productResponse = productMapper.toResponse(product);
        return new WishlistItemResponse(item.getId(), item.getUserId(), item.getCreatedAt(), productResponse);
    }
}
