package com.training.marketplace.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.training.marketplace.dto.response.CartItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the Redis serialization used by the cart {@code RedisTemplate} and the {@code @Cacheable}
 * product cache: values must deserialize back to their concrete DTO type, not a LinkedHashMap
 * (which previously caused a ClassCastException -> HTTP 500 on cache hits). Mirrors the mapper built
 * in {@link RedisConfig#createObjectMapper()}.
 */
class RedisSerializationTest {

    private GenericJackson2JsonRedisSerializer serializer() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY);
        return new GenericJackson2JsonRedisSerializer(mapper);
    }

    @Test
    void roundTripsRecordDtoToConcreteType() {
        GenericJackson2JsonRedisSerializer ser = serializer();
        CartItemResponse item = new CartItemResponse(
                5L, 1L, "WH1000XM5-BLK", "Sony WH-1000XM5", "Black",
                new BigDecimal("399.99"), 2, new BigDecimal("799.98"), null);

        Object back = ser.deserialize(ser.serialize(item));

        assertThat(back).isInstanceOf(CartItemResponse.class);
        CartItemResponse restored = (CartItemResponse) back;
        assertThat(restored.sku()).isEqualTo("WH1000XM5-BLK");
        assertThat(restored.subtotal()).isEqualByComparingTo("799.98");
    }
}
