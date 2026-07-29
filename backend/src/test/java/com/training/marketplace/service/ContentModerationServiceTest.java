package com.training.marketplace.service;

import com.training.marketplace.dto.request.UpdateModerationStatusRequest;
import com.training.marketplace.dto.response.ModerationItemResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductQuestion;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.ContentType;
import com.training.marketplace.enums.ModerationStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.repository.ModerationAuditLogRepository;
import com.training.marketplace.repository.ProductAnswerRepository;
import com.training.marketplace.repository.ProductQuestionRepository;
import com.training.marketplace.repository.ProductReviewRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.impl.ContentModerationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceTest {

    @Mock private ProductReviewRepository reviewRepository;
    @Mock private ProductQuestionRepository questionRepository;
    @Mock private ProductAnswerRepository answerRepository;
    @Mock private ModerationAuditLogRepository auditLogRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private ContentModerationServiceImpl moderationService;

    private User moderator;
    private ProductQuestion question;

    @BeforeEach
    void setUp() {
        moderator = User.builder().username("admin").build();
        moderator.setId(1L);

        Product product = Product.builder().name("Phone").build();
        product.setId(10L);

        User author = User.builder().username("author").build();
        author.setId(2L);

        question = ProductQuestion.builder().product(product).user(author).content("Inappropriate content").status(ModerationStatus.VISIBLE).build();
        question.setId(100L);
    }

    @Test
    void updateStatus_hideWithoutReason_throwsBadRequestException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(moderator));

        UpdateModerationStatusRequest request = new UpdateModerationStatusRequest(ContentType.QUESTION, 100L, ModerationStatus.HIDDEN, "");

        assertThatThrownBy(() -> moderationService.updateStatus(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Reason is required");
    }

    @Test
    void updateStatus_hideWithReason_updatesStatusAndLogsAudit() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(moderator));
        when(questionRepository.findById(100L)).thenReturn(Optional.of(question));

        UpdateModerationStatusRequest request = new UpdateModerationStatusRequest(ContentType.QUESTION, 100L, ModerationStatus.HIDDEN, "Spam content");
        ModerationItemResponse response = moderationService.updateStatus(1L, request);

        assertThat(response.status()).isEqualTo(ModerationStatus.HIDDEN);
        verify(questionRepository).save(question);
        verify(auditLogRepository).save(any());
    }
}
