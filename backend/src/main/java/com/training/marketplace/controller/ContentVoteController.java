package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.VoteRequest;
import com.training.marketplace.dto.response.VoteResponse;
import com.training.marketplace.service.ContentVoteService;
import com.training.marketplace.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/content")
@RequiredArgsConstructor
@Tag(name = "Content Votes", description = "Helpful vote endpoints for reviews, questions, answers")
public class ContentVoteController {

    private final ContentVoteService voteService;
    private final UserService userService;

    @PostMapping("/vote")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Toggle helpful vote for review, question, or answer")
    public ApiResponse<VoteResponse> toggleVote(
            @Valid @RequestBody VoteRequest request,
            Authentication authentication) {

        Long userId = userService.getAuthenticatedUser(authentication).getId();
        VoteResponse response = voteService.toggleVote(userId, request);
        return ApiResponse.success("Vote updated", response);
    }
}

