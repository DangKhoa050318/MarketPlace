package com.training.marketplace.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.dto.request.AddToCartRequest;
import com.training.marketplace.dto.response.CartItemResponse;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.service.CartService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis-hash cart keyed by userId; each field is a variantId. Price/name come from the variant
 * (+ product for display name). Stock is NOT enforced here — it is validated & reserved when the
 * order is placed (see OrderService / InventoryFacade), which is the authoritative no-oversell point.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String CART_KEY_PREFIX = "cart:";
    private static final long CART_TTL_DAYS = 7;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    @Override
    public CartResponse getCart(Long userId) {
        String key = getCartKey(userId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);

        List<CartItemResponse> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalItems = 0;

        for (Object value : entries.values()) {
            CartItemResponse item = convertToCartItem(value);
            if (item != null) {
                items.add(item);
                totalAmount = totalAmount.add(item.subtotal());
                totalItems += item.quantity();
            }
        }

        BigDecimal shippingFee = (totalAmount.compareTo(BigDecimal.ZERO) > 0
                && totalAmount.compareTo(new BigDecimal("150.00")) < 0)
                ? new BigDecimal("5.00")
                : BigDecimal.ZERO;

        refreshTtl(key);
        return new CartResponse(userId, items, totalAmount, shippingFee, totalItems);
    }

    @Override
    public CartResponse addItem(Long userId, AddToCartRequest request) {
        if (request.quantity() <= 0) {
            throw new BadRequestException("Quantity must be greater than 0");
        }

        ProductVariant variant = variantRepository.findById(request.variantId())
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", request.variantId()));
        if (!variant.isActive()) {
            throw new BadRequestException("Variant is inactive and cannot be added to cart");
        }

        String key = getCartKey(userId);
        String field = request.variantId().toString();
        Object existingValue = redisTemplate.opsForHash().get(key, field);

        int currentQuantity = 0;
        if (existingValue != null) {
            CartItemResponse existingItem = convertToCartItem(existingValue);
            if (existingItem != null) {
                currentQuantity = existingItem.quantity();
            }
        }

        int newQuantity = currentQuantity + request.quantity();
        redisTemplate.opsForHash().put(key, field, buildItem(variant, newQuantity));
        return getCart(userId);
    }

    @Override
    public CartResponse updateQuantity(Long userId, Long variantId, int quantity) {
        if (quantity <= 0) {
            removeItem(userId, variantId);
            return getCart(userId);
        }

        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", variantId));
        if (!variant.isActive()) {
            throw new BadRequestException("Variant is inactive");
        }

        redisTemplate.opsForHash().put(getCartKey(userId), variantId.toString(), buildItem(variant, quantity));
        return getCart(userId);
    }

    @Override
    public void removeItem(Long userId, Long variantId) {
        String key = getCartKey(userId);
        redisTemplate.opsForHash().delete(key, variantId.toString());
        refreshTtl(key);
    }

    @Override
    public void clearCart(Long userId) {
        redisTemplate.delete(getCartKey(userId));
    }

    private CartItemResponse buildItem(ProductVariant variant, int quantity) {
        Product product = productRepository.findById(variant.getProductId()).orElse(null);
        String productName = product != null ? product.getName() : variant.getVariantName();
        String imageUrl = variant.getImageUrl() != null
                ? variant.getImageUrl()
                : (product != null ? product.getImageUrl() : null);
        BigDecimal unitPrice = variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO;
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        return new CartItemResponse(variant.getId(), variant.getProductId(), variant.getSku(), productName,
                variant.getVariantName(), unitPrice, quantity, subtotal, imageUrl);
    }

    private String getCartKey(Long userId) {
        return CART_KEY_PREFIX + userId;
    }

    private void refreshTtl(String key) {
        redisTemplate.expire(key, CART_TTL_DAYS, TimeUnit.DAYS);
    }

    private CartItemResponse convertToCartItem(Object obj) {
        if (obj == null) return null;
        if (obj instanceof CartItemResponse cartItemResponse) {
            return cartItemResponse;
        }
        try {
            if (obj instanceof String jsonString) {
                return objectMapper.readValue(jsonString, CartItemResponse.class);
            }
            return objectMapper.convertValue(obj, CartItemResponse.class);
        } catch (Exception e) {
            log.error("Failed to convert object to CartItemResponse: {}", obj, e);
            return null;
        }
    }
}
