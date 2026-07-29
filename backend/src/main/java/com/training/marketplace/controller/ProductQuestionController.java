package com.training.marketplace.controller;

import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.dto.request.CreateAnswerRequest;
import com.training.marketplace.dto.request.CreateQuestionRequest;
import com.training.marketplace.dto.response.AnswerResponse;
import com.training.marketplace.dto.response.QuestionResponse;
import com.training.marketplace.entity.User;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.ProductQuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Product Questions", description = "Product Q&A endpoints")
public class ProductQuestionController {

    private final ProductQuestionService questionService;
    private final UserRepository userRepository;

    @GetMapping("/products/{productId}/questions")
    @Operation(summary = "Get Q&A thread for product (Public)")
    public ResponseEntity<ApiResponse<Page<QuestionResponse>>> getQuestions(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long userId = resolveOptionalUserId(authentication);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<QuestionResponse> questions = questionService.getQuestionsByProduct(productId, userId, pageable);
        return ResponseEntity.ok(ApiResponse.success(questions));
    }

    @PostMapping("/products/{productId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Ask a question about a product")
    public ResponseEntity<ApiResponse<QuestionResponse>> createQuestion(
            @PathVariable Long productId,
            @Valid @RequestBody CreateQuestionRequest request,
            Authentication authentication) {

        Long userId = getRequiredUserId(authentication);
        QuestionResponse response = questionService.createQuestion(productId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Question submitted successfully", response));
    }

    @PostMapping("/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Answer a question")
    public ResponseEntity<ApiResponse<AnswerResponse>> createAnswer(
            @PathVariable Long questionId,
            @Valid @RequestBody CreateAnswerRequest request,
            Authentication authentication) {

        Long userId = getRequiredUserId(authentication);
        AnswerResponse response = questionService.createAnswer(questionId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Answer submitted successfully", response));
    }

    @PostMapping("/answers/{answerId}/official")
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')")
    @Operation(summary = "Mark answer as official (STAFF/MANAGER/ADMIN)")
    public ResponseEntity<ApiResponse<AnswerResponse>> markOfficial(
            @PathVariable Long answerId,
            Authentication authentication) {

        Long userId = getRequiredUserId(authentication);
        AnswerResponse response = questionService.markAnswerOfficial(answerId, userId);
        return ResponseEntity.ok(ApiResponse.success("Answer marked as official", response));
    }

    private Long resolveOptionalUserId(Authentication authentication) {
        Authentication auth = authentication != null ? authentication : SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            return null;
        }
        return userRepository.findByUsername(auth.getName())
                .or(() -> userRepository.findByEmail(auth.getName()))
                .map(User::getId)
                .orElse(null);
    }

    private Long getRequiredUserId(Authentication authentication) {
        Long userId = resolveOptionalUserId(authentication);
        if (userId == null) {
            throw new ResourceNotFoundException("Unauthenticated user context");
        }
        return userId;
    }
}
