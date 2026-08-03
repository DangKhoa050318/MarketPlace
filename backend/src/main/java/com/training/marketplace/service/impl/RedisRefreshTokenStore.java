package com.training.marketplace.service.impl;

import com.training.marketplace.service.RefreshTokenStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";

    private static final DefaultRedisScript<Long> ROTATE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current ~= ARGV[1] then
                return 0
            end
            redis.call('PSETEX', KEYS[1], ARGV[3], ARGV[2])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> REVOKE_SCRIPT = new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            if current ~= ARGV[1] then
                return 0
            end
            return redis.call('DEL', KEYS[1])
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String username, String tokenId, Duration ttl) {
        redisTemplate.opsForValue().set(key(username), tokenId, ttl);
    }

    @Override
    public boolean rotate(String username, String currentTokenId, String newTokenId, Duration ttl) {
        Long result = redisTemplate.execute(
                ROTATE_SCRIPT,
                List.of(key(username)),
                currentTokenId,
                newTokenId,
                Long.toString(ttl.toMillis()));
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public void revoke(String username, String tokenId) {
        redisTemplate.execute(REVOKE_SCRIPT, List.of(key(username)), tokenId);
    }

    private String key(String username) {
        return KEY_PREFIX + username;
    }
}
