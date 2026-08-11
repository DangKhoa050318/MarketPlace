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
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    public static final int MAX_REQUESTS_PER_MINUTE = 100;
    /** Tighter cap for the login endpoint to slow credential brute-forcing (#L). */
    public static final int LOGIN_MAX_REQUESTS_PER_MINUTE = 5;
    public static final int WINDOW_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // if ("true".equalsIgnoreCase(request.getHeader("X-Bypass-Rate-Limit"))) {
        //     return true;
        // }
        return path.startsWith("/api/v1/webhooks");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        boolean isLogin = "POST".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().endsWith("/api/v1/auth/login");
        int limit = isLogin ? LOGIN_MAX_REQUESTS_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;
        // Login is pre-auth → key by IP (brute-force is per source). Other traffic keys by the
        // authenticated username when available so users behind a shared NAT IP aren't punished (#J).
        String scope = isLogin ? "login:" + extractClientIp(request) : resolveIdentity(request);
        String key = "rate_limit:" + scope;

        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }

            Long expireSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            long resetSeconds = (expireSeconds != null && expireSeconds > 0) ? expireSeconds : WINDOW_SECONDS;
            long currentCount = (count != null) ? count : 1;

            response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(Math.max(0, limit - currentCount)));
            response.setHeader("X-RateLimit-Reset", String.valueOf(resetSeconds));

            if (currentCount > limit) {
                log.warn("Rate limit exceeded for {}: {} requests in {}s window", scope, currentCount, WINDOW_SECONDS);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                ApiResponse<Void> errorResponse = ApiResponse.error(
                        "Rate limit exceeded. Try again in " + resetSeconds + "s."
                );
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }
        } catch (Exception e) {
            log.warn("RateLimitingFilter: Redis rate limiting unavailable ({}), bypassing check", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /** Authenticated username when the security context is populated, otherwise the client IP. */
    private String resolveIdentity(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getName() != null) {
            return "user:" + auth.getName();
        }
        return "ip:" + extractClientIp(request);
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
