package com.training.marketplace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.dto.request.AddToCartRequest;
import com.training.marketplace.dto.response.CartItemResponse;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.service.impl.CartServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ProductVariantRepository variantRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private CartServiceImpl cartService;

    @Test
    void getCart_emptyCart_returnsEmptyCartResponse() {
        Long userId = 1L;
        String key = "cart:1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.entries(key)).thenReturn(Map.of());

        var response = cartService.getCart(userId);

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.items()).isEmpty();
        assertThat(response.totalAmount()).isEqualTo(BigDecimal.ZERO);
        assertThat(response.totalItems()).isEqualTo(0);
        verify(redisTemplate).expire(key, 7, TimeUnit.DAYS);
    }

    @Test
    void addItem_validVariant_addsToCartAndRefreshesTtl() {
        Long userId = 1L;
        Long variantId = 10L;
        String key = "cart:1";
        var request = new AddToCartRequest(variantId, 2);
        var variant = buildVariant(variantId, "Silver / 16GB", BigDecimal.valueOf(50), true);

        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(hashOperations.get(key, "10")).thenReturn(null);
        when(hashOperations.entries(key)).thenReturn(Map.of());

        var response = cartService.addItem(userId, request);

        assertThat(response.userId()).isEqualTo(userId);
        verify(hashOperations).put(eq(key), eq("10"), any(CartItemResponse.class));
        verify(redisTemplate).expire(key, 7, TimeUnit.DAYS);
    }

    @Test
    void addItem_inactiveVariant_throwsBadRequestException() {
        Long userId = 1L;
        Long variantId = 10L;
        var request = new AddToCartRequest(variantId, 1);
        var variant = buildVariant(variantId, "Silver", BigDecimal.valueOf(50), false);

        when(variantRepository.findById(variantId)).thenReturn(Optional.of(variant));

        assertThatThrownBy(() -> cartService.addItem(userId, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    void removeItem_deletesHashKeyAndRefreshesTtl() {
        Long userId = 1L;
        Long variantId = 10L;
        String key = "cart:1";
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        cartService.removeItem(userId, variantId);

        verify(hashOperations).delete(key, "10");
        verify(redisTemplate).expire(key, 7, TimeUnit.DAYS);
    }

    @Test
    void clearCart_deletesRedisKey() {
        cartService.clearCart(1L);
        verify(redisTemplate).delete("cart:1");
    }

    @Test
    void testCartItemResponseRedisSerializationWithDefaultTyping() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.activateDefaultTyping(
                com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator.builder()
                        .allowIfSubType("com.training.marketplace.")
                        .allowIfSubType("java.lang.")
                        .allowIfSubType("java.util.")
                        .allowIfSubType("java.time.")
                        .allowIfSubType("java.math.")
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer serializer =
                new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer(mapper);

        CartItemResponse item = new CartItemResponse(
                10L, "SKU-10", "Phone", "Silver",
                BigDecimal.valueOf(500), 2, BigDecimal.valueOf(1000), "http://img.jpg"
        );

        byte[] bytes = serializer.serialize(item);
        Object deserialized = serializer.deserialize(bytes);
        System.out.println("Deserialized class: " + deserialized.getClass());

        Map<String, Object> map = new java.util.HashMap<>();
        map.put("@class", "com.training.marketplace.dto.response.CartItemResponse");
        map.put("variantId", 10L);
        map.put("sku", "SKU-10");
        map.put("productName", "Phone");
        map.put("variantName", "Silver");
        map.put("unitPrice", BigDecimal.valueOf(500));
        map.put("quantity", 2);
        map.put("subtotal", BigDecimal.valueOf(1000));
        map.put("imageUrl", "http://img.jpg");

        CartItemResponse converted = new ObjectMapper().convertValue(map, CartItemResponse.class);
        assertThat(converted).isNotNull();
    }

    private ProductVariant buildVariant(Long id, String variantName, BigDecimal price, boolean active) {
        ProductVariant variant = ProductVariant.builder()
                .productId(1L).sku("SKU-" + id).variantName(variantName).price(price).active(active).build();
        variant.setId(id);
        return variant;
    }
}
