package com.training.marketplace.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private FilterChain filterChain;

    private RateLimitingFilter rateLimitingFilter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        rateLimitingFilter = new RateLimitingFilter(redisTemplate, objectMapper);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilter_underLimit_allowsRequestAndSetsHeaders() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate_limit:ip:192.168.1.100")).thenReturn(5L);
        when(redisTemplate.getExpire("rate_limit:ip:192.168.1.100", TimeUnit.SECONDS)).thenReturn(45L);

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("95");
        assertThat(response.getHeader("X-RateLimit-Reset")).isEqualTo("45");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_firstRequest_setsKeyExpiration() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate_limit:ip:10.0.0.1")).thenReturn(1L);
        when(redisTemplate.getExpire("rate_limit:ip:10.0.0.1", TimeUnit.SECONDS)).thenReturn(60L);

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(redisTemplate).expire("rate_limit:ip:10.0.0.1", 60, TimeUnit.SECONDS);
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_exceedsLimit_returns429TooManyRequests() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.200");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate_limit:ip:192.168.1.200")).thenReturn(101L);
        when(redisTemplate.getExpire("rate_limit:ip:192.168.1.200", TimeUnit.SECONDS)).thenReturn(30L);

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        assertThat(response.getContentAsString()).contains("Rate limit exceeded");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_xForwardedForHeader_usesFirstIp() throws ServletException, IOException {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(valueOperations.increment("rate_limit:ip:203.0.113.195")).thenReturn(10L);
        when(redisTemplate.getExpire("rate_limit:ip:203.0.113.195", TimeUnit.SECONDS)).thenReturn(50L);

        // When
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        // Then
        verify(valueOperations).increment("rate_limit:ip:203.0.113.195");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_authenticatedRequest_usesUsernameInsteadOfSharedIp() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "alice", "n/a", java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOperations.increment("rate_limit:user:alice")).thenReturn(2L);
        when(redisTemplate.getExpire("rate_limit:user:alice", TimeUnit.SECONDS)).thenReturn(40L);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(valueOperations).increment("rate_limit:user:alice");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_chatEndpoint_usesTighterChatLimitAndOwnBucket() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat/messages");
        request.setRemoteAddr("192.168.1.77");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOperations.increment("rate_limit:chat:ip:192.168.1.77")).thenReturn(3L);
        when(redisTemplate.getExpire("rate_limit:chat:ip:192.168.1.77", TimeUnit.SECONDS)).thenReturn(50L);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("20");
        assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("17");
        verify(valueOperations).increment("rate_limit:chat:ip:192.168.1.77");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilter_21stChatRequest_returns429WithChatLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/chat/messages");
        request.setRemoteAddr("192.168.1.78");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOperations.increment("rate_limit:chat:ip:192.168.1.78")).thenReturn(21L);
        when(redisTemplate.getExpire("rate_limit:chat:ip:192.168.1.78", TimeUnit.SECONDS)).thenReturn(30L);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("20");
        verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    void doFilter_sixthLoginRequest_returns429WithLoginLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.168.1.50");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(valueOperations.increment("rate_limit:login:192.168.1.50")).thenReturn(6L);
        when(redisTemplate.getExpire("rate_limit:login:192.168.1.50", TimeUnit.SECONDS)).thenReturn(30L);

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        verify(filterChain, never()).doFilter(request, response);
    }
}
