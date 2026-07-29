package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.VoteRequest;
import com.training.marketplace.dto.response.VoteResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.ContentVoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    private final UserRepository userRepository;

    @PostMapping("/vote")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Toggle helpful vote for review, question, or answer")
    public ResponseEntity<ApiResponse<VoteResponse>> toggleVote(
            @Valid @RequestBody VoteRequest request,
            Authentication authentication) {

        Long userId = getRequiredUserId(authentication);
        VoteResponse response = voteService.toggleVote(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Vote updated", response));
    }

    private Long getRequiredUserId(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new ResourceNotFoundException("Unauthenticated user context");
        }
        String name = auth.getName();
        return userRepository.findByUsername(name)
                .or(() -> userRepository.findByEmail(name))
                .map(User::getId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + name));
    }
}
