package com.training.marketplace.service.impl;

import com.training.marketplace.dto.response.ProductResponse;
import com.training.marketplace.dto.response.RecentlyViewedProductResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.RecentlyViewedProduct;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.mapper.ProductMapper;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.RecentlyViewedProductRepository;
import com.training.marketplace.service.RecentlyViewedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecentlyViewedProductServiceImpl implements RecentlyViewedProductService {

    private static final int STORED_LIMIT = 20;

    private final RecentlyViewedProductRepository recentlyViewedRepository;
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public RecentlyViewedProductResponse record(Long userId, String sessionId, Long productId) {
        Owner owner = resolveOwner(userId, sessionId);
        Product product = productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        RecentlyViewedProduct viewed = owner.userId() != null
                ? recentlyViewedRepository.findByUserIdAndProductId(owner.userId(), productId)
                        .orElseGet(() -> RecentlyViewedProduct.builder()
                                .userId(owner.userId())
                                .productId(productId)
                                .build())
                : recentlyViewedRepository.findBySessionIdAndProductId(owner.sessionId(), productId)
                        .orElseGet(() -> RecentlyViewedProduct.builder()
                                .sessionId(owner.sessionId())
                                .productId(productId)
                                .build());

        viewed.setViewedAt(LocalDateTime.now());
        RecentlyViewedProduct saved = recentlyViewedRepository.save(viewed);
        trimOverflow(owner);
        return toResponse(saved, product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentlyViewedProductResponse> list(Long userId, String sessionId, int limit) {
        Owner owner = resolveOwner(userId, sessionId);
        int boundedLimit = Math.max(1, Math.min(limit, STORED_LIMIT));
        List<RecentlyViewedProduct> views = owner.userId() != null
                ? recentlyViewedRepository.findRecentByUserId(owner.userId(), PageRequest.of(0, boundedLimit))
                : recentlyViewedRepository.findRecentBySessionId(owner.sessionId(), PageRequest.of(0, boundedLimit));

        Map<Long, Product> productsById = productRepository.findAllById(views.stream()
                        .map(RecentlyViewedProduct::getProductId)
                        .collect(Collectors.toSet()))
                .stream()
                .filter(Product::isActive)
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return views.stream()
                .filter(view -> productsById.containsKey(view.getProductId()))
                .map(view -> toResponse(view, productsById.get(view.getProductId())))
                .toList();
    }

    @Override
    @Transactional
    public void clear(Long userId, String sessionId) {
        Owner owner = resolveOwner(userId, sessionId);
        if (owner.userId() != null) {
            recentlyViewedRepository.deleteByUserId(owner.userId());
            return;
        }
        recentlyViewedRepository.deleteBySessionId(owner.sessionId());
    }

    private void trimOverflow(Owner owner) {
        if (owner.userId() != null) {
            recentlyViewedRepository.deleteUserOverflow(owner.userId(), STORED_LIMIT);
            return;
        }
        recentlyViewedRepository.deleteSessionOverflow(owner.sessionId(), STORED_LIMIT);
    }

    private RecentlyViewedProductResponse toResponse(RecentlyViewedProduct viewed, Product product) {
        ProductResponse productResponse = productMapper.toResponse(product);
        return new RecentlyViewedProductResponse(viewed.getId(), viewed.getViewedAt(), productResponse);
    }

    private Owner resolveOwner(Long userId, String sessionId) {
        if (userId != null) {
            return new Owner(userId, null);
        }
        if (sessionId == null || sessionId.isBlank()) {
            throw new BadRequestException("X-Session-Id is required for anonymous recently viewed products");
        }
        String normalizedSessionId = sessionId.trim();
        if (normalizedSessionId.length() > 128) {
            throw new BadRequestException("X-Session-Id must not exceed 128 characters");
        }
        return new Owner(null, normalizedSessionId);
    }

    private record Owner(Long userId, String sessionId) {
    }
}
