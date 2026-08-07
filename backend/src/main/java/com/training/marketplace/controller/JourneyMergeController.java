package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.MergeAnonymousJourneyRequest;
import com.training.marketplace.dto.response.JourneyMergeResponse;
import com.training.marketplace.service.AnonymousJourneyMergeService;
import com.training.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/journey")
@RequiredArgsConstructor
public class JourneyMergeController {

    private final AnonymousJourneyMergeService anonymousJourneyMergeService;
    private final UserService userService;

    @PostMapping("/merge")
    @Operation(summary = "Merge anonymous recently viewed and analytics events into the authenticated user")
    public ApiResponse<JourneyMergeResponse> merge(
            Authentication authentication,
            @Valid @RequestBody MergeAnonymousJourneyRequest request) {
        Long userId = userService.getAuthenticatedUser(authentication).getId();
        return ApiResponse.success("Anonymous journey merged",
                anonymousJourneyMergeService.merge(userId, request.sessionId()));
    }
}

