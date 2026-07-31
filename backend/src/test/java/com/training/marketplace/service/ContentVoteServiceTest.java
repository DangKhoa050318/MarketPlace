package com.training.marketplace.service;

import com.training.marketplace.dto.request.VoteRequest;
import com.training.marketplace.dto.response.VoteResponse;
import com.training.marketplace.entity.ContentHelpfulVote;
import com.training.marketplace.entity.ProductQuestion;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.ContentType;
import com.training.marketplace.repository.ContentHelpfulVoteRepository;
import com.training.marketplace.repository.ProductAnswerRepository;
import com.training.marketplace.repository.ProductQuestionRepository;
import com.training.marketplace.repository.ProductReviewRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.impl.ContentVoteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentVoteServiceTest {

    @Mock private ContentHelpfulVoteRepository voteRepository;
    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductQuestionRepository questionRepository;
    @Mock private ProductAnswerRepository answerRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ContentVoteServiceImpl voteService;

    private User user;
    private ProductQuestion question;

    @BeforeEach
    void setUp() {
        user = User.builder().username("alice").build();
        user.setId(5L);

        question = ProductQuestion.builder().content("Is battery included?").helpfulCount(2L).build();
        question.setId(10L);
    }

    @Test
    void toggleVote_firstTime_incrementsCountAndSavesVote() {
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(voteRepository.findByUserIdAndTargetTypeAndTargetId(5L, ContentType.QUESTION, 10L)).thenReturn(Optional.empty());
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        VoteRequest request = new VoteRequest(ContentType.QUESTION, 10L);
        VoteResponse response = voteService.toggleVote(5L, request);

        assertThat(response.isVoted()).isTrue();
        assertThat(response.helpfulCount()).isEqualTo(3L);
        verify(voteRepository).save(any(ContentHelpfulVote.class));
        verify(questionRepository).save(question);
    }

    @Test
    void toggleVote_secondTime_decrementsCountAndDeletesVote() {
        ContentHelpfulVote existing = ContentHelpfulVote.builder().user(user).targetType(ContentType.QUESTION).targetId(10L).build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(voteRepository.findByUserIdAndTargetTypeAndTargetId(5L, ContentType.QUESTION, 10L)).thenReturn(Optional.of(existing));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));

        VoteRequest request = new VoteRequest(ContentType.QUESTION, 10L);
        VoteResponse response = voteService.toggleVote(5L, request);

        assertThat(response.isVoted()).isFalse();
        assertThat(response.helpfulCount()).isEqualTo(1L);
        verify(voteRepository).delete(existing);
        verify(questionRepository).save(question);
    }
}
