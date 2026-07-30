package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.MergeAnonymousJourneyRequest;
import com.training.marketplace.dto.response.JourneyMergeResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.AnonymousJourneyMergeService;
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
    private final UserRepository userRepository;

    @PostMapping("/merge")
    @Operation(summary = "Merge anonymous recently viewed and analytics events into the authenticated user")
    public ApiResponse<JourneyMergeResponse> merge(
            Authentication authentication,
            @Valid @RequestBody MergeAnonymousJourneyRequest request) {
        return ApiResponse.success("Anonymous journey merged",
                anonymousJourneyMergeService.merge(currentUserId(authentication), request.sessionId()));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadRequestException("Authenticated user is required");
        }
        return userRepository.findByUsername(authentication.getName())
                .or(() -> userRepository.findByEmail(authentication.getName()))
                .map(User::getId)
                .orElseThrow(() -> new BadRequestException("Authenticated user could not be resolved"));
    }
}
