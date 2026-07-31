package com.training.marketplace.service.impl;

import com.training.marketplace.dto.response.WishlistStatusResponse;
import com.training.marketplace.entity.AnonymousWishlistItem;
import com.training.marketplace.entity.Product;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.AnonymousWishlistItemRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.service.AnonymousWishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnonymousWishlistServiceImpl implements AnonymousWishlistService {

    private final AnonymousWishlistItemRepository anonymousWishlistRepository;
    private final ProductRepository productRepository;

    @Override
    @Transactional
    public WishlistStatusResponse add(String sessionId, Long productId) {
        String normalizedSessionId = normalizeSessionId(sessionId);
        findActiveProduct(productId);
        anonymousWishlistRepository.findBySessionIdAndProductId(normalizedSessionId, productId)
                .orElseGet(() -> anonymousWishlistRepository.save(AnonymousWishlistItem.builder()
                        .sessionId(normalizedSessionId)
                        .productId(productId)
                        .build()));
        return new WishlistStatusResponse(productId, true);
    }

    @Override
    @Transactional
    public void remove(String sessionId, Long productId) {
        anonymousWishlistRepository.deleteBySessionIdAndProductId(normalizeSessionId(sessionId), productId);
    }

    @Override
    @Transactional(readOnly = true)
    public WishlistStatusResponse status(String sessionId, Long productId) {
        boolean wishlisted = anonymousWishlistRepository.existsBySessionIdAndProductId(
                normalizeSessionId(sessionId),
                productId);
        return new WishlistStatusResponse(productId, wishlisted);
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank() || sessionId.length() > 128) {
            throw new BadRequestException("Valid anonymous session ID is required");
        }
        return sessionId.trim();
    }

    private Product findActiveProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }
}
