package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.CreateAnswerRequest;
import com.training.marketplace.dto.request.CreateQuestionRequest;
import com.training.marketplace.dto.response.AnswerResponse;
import com.training.marketplace.dto.response.QuestionResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductAnswer;
import com.training.marketplace.entity.ProductQuestion;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.ContentType;
import com.training.marketplace.enums.ModerationStatus;
import com.training.marketplace.enums.Role;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ContentHelpfulVoteRepository;
import com.training.marketplace.repository.ProductAnswerRepository;
import com.training.marketplace.repository.ProductQuestionRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.ProductQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductQuestionServiceImpl implements ProductQuestionService {

    private final ProductQuestionRepository questionRepository;
    private final ProductAnswerRepository answerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ContentHelpfulVoteRepository voteRepository;

    @Override
    @Transactional
    public QuestionResponse createQuestion(Long productId, Long userId, CreateQuestionRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        ProductQuestion question = ProductQuestion.builder()
                .product(product)
                .user(user)
                .content(request.content().trim())
                .status(ModerationStatus.VISIBLE)
                .helpfulCount(0L)
                .build();

        ProductQuestion saved = questionRepository.save(question);
        return mapToQuestionResponse(saved, false, Collections.emptyList());
    }

    @Override
    @Transactional
    public AnswerResponse createAnswer(Long questionId, Long userId, CreateAnswerRequest request) {
        ProductQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductQuestion", questionId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        boolean isStaffOrAdmin = user.getRole() == Role.ADMIN || user.getRole() == Role.MANAGER || user.getRole() == Role.STAFF;

        ProductAnswer answer = ProductAnswer.builder()
                .question(question)
                .user(user)
                .content(request.content().trim())
                .isOfficial(isStaffOrAdmin)
                .status(ModerationStatus.VISIBLE)
                .helpfulCount(0L)
                .build();

        ProductAnswer saved = answerRepository.save(answer);
        return mapToAnswerResponse(saved, false);
    }

    @Override
    @Transactional
    public AnswerResponse markAnswerOfficial(Long answerId, Long currentUserId) {
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.MANAGER && currentUser.getRole() != Role.STAFF) {
            throw new AccessDeniedException("Only staff or admin members can mark answers as official");
        }

        ProductAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("ProductAnswer", answerId));

        answer.setIsOfficial(true);
        ProductAnswer updated = answerRepository.save(answer);

        boolean isVoted = voteRepository.existsByUserIdAndTargetTypeAndTargetId(currentUserId, ContentType.ANSWER, answerId);
        return mapToAnswerResponse(updated, isVoted);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<QuestionResponse> getQuestionsByProduct(Long productId, Long currentUserId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product", productId);
        }

        // Only return VISIBLE questions in public API
        Page<ProductQuestion> questionPage = questionRepository.findByProductIdAndStatus(productId, ModerationStatus.VISIBLE, pageable);
        List<Long> questionIds = questionPage.getContent().stream().map(ProductQuestion::getId).toList();

        if (questionIds.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, questionPage.getTotalElements());
        }

        // Batch load answers with VISIBLE status
        List<ProductAnswer> allAnswers = answerRepository.findByQuestionIdInAndStatusOrderByIsOfficialDescCreatedAtAsc(questionIds, ModerationStatus.VISIBLE);
        Map<Long, List<ProductAnswer>> answersByQuestionId = allAnswers.stream()
                .collect(Collectors.groupingBy(a -> a.getQuestion().getId()));

        // Batch load votes for current user if logged in
        Set<Long> votedQuestionIds = Collections.emptySet();
        Set<Long> votedAnswerIds = Collections.emptySet();

        if (currentUserId != null) {
            votedQuestionIds = voteRepository.findByUserIdAndTargetTypeAndTargetIdIn(currentUserId, ContentType.QUESTION, questionIds)
                    .stream().map(v -> v.getTargetId()).collect(Collectors.toSet());

            List<Long> allAnswerIds = allAnswers.stream().map(ProductAnswer::getId).toList();
            if (!allAnswerIds.isEmpty()) {
                votedAnswerIds = voteRepository.findByUserIdAndTargetTypeAndTargetIdIn(currentUserId, ContentType.ANSWER, allAnswerIds)
                        .stream().map(v -> v.getTargetId()).collect(Collectors.toSet());
            }
        }

        final Set<Long> finalVotedQuestionIds = votedQuestionIds;
        final Set<Long> finalVotedAnswerIds = votedAnswerIds;

        List<QuestionResponse> dtos = questionPage.getContent().stream().map(q -> {
            List<ProductAnswer> qAnswers = answersByQuestionId.getOrDefault(q.getId(), Collections.emptyList());
            List<AnswerResponse> answerDtos = qAnswers.stream()
                    .map(a -> mapToAnswerResponse(a, finalVotedAnswerIds.contains(a.getId())))
                    .toList();

            return mapToQuestionResponse(q, finalVotedQuestionIds.contains(q.getId()), answerDtos);
        }).toList();

        return new PageImpl<>(dtos, pageable, questionPage.getTotalElements());
    }

    private QuestionResponse mapToQuestionResponse(ProductQuestion q, boolean isVoted, List<AnswerResponse> answers) {
        String userName = q.getUser() != null ? (q.getUser().getFullName() != null ? q.getUser().getFullName() : q.getUser().getUsername()) : "Anonymous";
        return new QuestionResponse(
                q.getId(),
                q.getProduct().getId(),
                q.getUser().getId(),
                userName,
                q.getContent(),
                q.getStatus(),
                q.getHelpfulCount(),
                isVoted,
                answers,
                q.getCreatedAt()
        );
    }

    private AnswerResponse mapToAnswerResponse(ProductAnswer a, boolean isVoted) {
        String userName = a.getUser() != null ? (a.getUser().getFullName() != null ? a.getUser().getFullName() : a.getUser().getUsername()) : "Anonymous";
        return new AnswerResponse(
                a.getId(),
                a.getQuestion().getId(),
                a.getUser().getId(),
                userName,
                a.getContent(),
                a.getIsOfficial(),
                a.getStatus(),
                a.getHelpfulCount(),
                isVoted,
                a.getCreatedAt()
        );
    }
}
