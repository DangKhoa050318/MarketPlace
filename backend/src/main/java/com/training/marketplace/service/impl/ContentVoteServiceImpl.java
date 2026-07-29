package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.VoteRequest;
import com.training.marketplace.dto.response.VoteResponse;
import com.training.marketplace.entity.ContentHelpfulVote;
import com.training.marketplace.entity.ProductAnswer;
import com.training.marketplace.entity.ProductQuestion;
import com.training.marketplace.entity.ProductReview;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.ContentType;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ContentHelpfulVoteRepository;
import com.training.marketplace.repository.ProductAnswerRepository;
import com.training.marketplace.repository.ProductQuestionRepository;
import com.training.marketplace.repository.ProductReviewRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.ContentVoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ContentVoteServiceImpl implements ContentVoteService {

    private final ContentHelpfulVoteRepository voteRepository;
    private final ProductReviewRepository reviewRepository;
    private final ProductQuestionRepository questionRepository;
    private final ProductAnswerRepository answerRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public VoteResponse toggleVote(Long userId, VoteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        ContentType type = request.targetType();
        Long targetId = request.targetId();

        Optional<ContentHelpfulVote> existingVote = voteRepository.findByUserIdAndTargetTypeAndTargetId(userId, type, targetId);
        boolean willBeVoted = existingVote.isEmpty();
        long newHelpfulCount = 0L;

        if (willBeVoted) {
            ContentHelpfulVote newVote = ContentHelpfulVote.builder()
                    .user(user)
                    .targetType(type)
                    .targetId(targetId)
                    .build();
            voteRepository.save(newVote);
        } else {
            voteRepository.delete(existingVote.get());
        }

        switch (type) {
            case REVIEW -> {
                ProductReview review = reviewRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("ProductReview", targetId));
                long count = review.getHelpfulCount() != null ? review.getHelpfulCount() : 0L;
                newHelpfulCount = willBeVoted ? count + 1 : Math.max(0, count - 1);
                review.setHelpfulCount(newHelpfulCount);
                reviewRepository.save(review);
            }
            case QUESTION -> {
                ProductQuestion question = questionRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("ProductQuestion", targetId));
                long count = question.getHelpfulCount() != null ? question.getHelpfulCount() : 0L;
                newHelpfulCount = willBeVoted ? count + 1 : Math.max(0, count - 1);
                question.setHelpfulCount(newHelpfulCount);
                questionRepository.save(question);
            }
            case ANSWER -> {
                ProductAnswer answer = answerRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("ProductAnswer", targetId));
                long count = answer.getHelpfulCount() != null ? answer.getHelpfulCount() : 0L;
                newHelpfulCount = willBeVoted ? count + 1 : Math.max(0, count - 1);
                answer.setHelpfulCount(newHelpfulCount);
                answerRepository.save(answer);
            }
        }

        return new VoteResponse(type, targetId, newHelpfulCount, willBeVoted);
    }
}
