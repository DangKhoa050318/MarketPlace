package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.TrackAnalyticsEventRequest;
import com.training.marketplace.dto.response.AnalyticsEventResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.AnalyticsEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics/events")
@RequiredArgsConstructor
@Tag(name = "Analytics Events", description = "Canonical storefront event taxonomy")
public class AnalyticsEventController {

    private final AnalyticsEventService analyticsEventService;
    private final UserRepository userRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
            summary = "Ingest a canonical storefront analytics event",
            description = """
                    Durably stores a schema-v1 browser event. Authenticated user identity is
                    derived from JWT and anonymous session identity from X-Session-Id.
                    PURCHASE is server-generated and is not accepted from this public endpoint.
                    """)
    public ApiResponse<AnalyticsEventResponse> track(
            Authentication authentication,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody TrackAnalyticsEventRequest request) {
        return ApiResponse.success("Analytics event accepted",
                analyticsEventService.track(currentUserId(authentication), sessionId, request));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()))
                .map(User::getId)
                .orElse(null);
    }
}
