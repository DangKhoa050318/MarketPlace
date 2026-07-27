package com.training.marketplace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.training.marketplace.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    public static final int MAX_REQUESTS_PER_MINUTE = 100;
    public static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = extractClientIp(request);
        String key = "rate_limit:" + clientIp;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        Long expireSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long resetSeconds = (expireSeconds != null && expireSeconds > 0) ? expireSeconds : WINDOW_SECONDS;
        long currentCount = (count != null) ? count : 1;

        response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_MINUTE));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, MAX_REQUESTS_PER_MINUTE - currentCount)));
        response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));

        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for IP {}: {} requests in 60s window", clientIp, currentCount);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ApiResponse<Void> errorResponse = ApiResponse.error(
                    "Rate limit exceeded. Maximum 100 requests per minute allowed."
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
