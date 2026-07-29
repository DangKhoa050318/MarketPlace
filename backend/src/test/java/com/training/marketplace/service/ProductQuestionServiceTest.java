package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateAnswerRequest;
import com.training.marketplace.dto.request.CreateQuestionRequest;
import com.training.marketplace.dto.response.AnswerResponse;
import com.training.marketplace.dto.response.QuestionResponse;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductAnswer;
import com.training.marketplace.entity.ProductQuestion;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.ModerationStatus;
import com.training.marketplace.enums.Role;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ContentHelpfulVoteRepository;
import com.training.marketplace.repository.ProductAnswerRepository;
import com.training.marketplace.repository.ProductQuestionRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.impl.ProductQuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductQuestionServiceTest {

    @Mock private ProductQuestionRepository questionRepository;
    @Mock private ProductAnswerRepository answerRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentHelpfulVoteRepository voteRepository;

    @InjectMocks private ProductQuestionServiceImpl questionService;

    private Product product;
    private User customerUser;
    private User staffUser;
    private ProductQuestion sampleQuestion;
    private ProductAnswer sampleAnswer;

    @BeforeEach
    void setUp() {
        product = Product.builder().slug("laptop").name("Laptop").build();
        product.setId(1L);

        customerUser = User.builder().username("john").email("john@example.com").role(Role.CUSTOMER).fullName("John Doe").build();
        customerUser.setId(10L);

        staffUser = User.builder().username("staff").email("staff@example.com").role(Role.STAFF).fullName("Staff Member").build();
        staffUser.setId(20L);

        sampleQuestion = ProductQuestion.builder()
                .product(product)
                .user(customerUser)
                .content("Does this laptop support RAM upgrade?")
                .status(ModerationStatus.VISIBLE)
                .helpfulCount(3L)
                .build();
        sampleQuestion.setId(100L);

        sampleAnswer = ProductAnswer.builder()
                .question(sampleQuestion)
                .user(staffUser)
                .content("Yes, it supports up to 32GB RAM.")
                .isOfficial(true)
                .status(ModerationStatus.VISIBLE)
                .helpfulCount(5L)
                .build();
        sampleAnswer.setId(200L);
    }

    @Test
    void createQuestion_validRequest_returnsQuestionResponse() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));
        when(questionRepository.save(any(ProductQuestion.class))).thenReturn(sampleQuestion);

        CreateQuestionRequest request = new CreateQuestionRequest("Does this laptop support RAM upgrade?");
        QuestionResponse response = questionService.createQuestion(1L, 10L, request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.content()).isEqualTo("Does this laptop support RAM upgrade?");
        assertThat(response.userName()).isEqualTo("John Doe");
    }

    @Test
    void createQuestion_productNotFound_throwsException() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        CreateQuestionRequest request = new CreateQuestionRequest("Is it good?");

        assertThatThrownBy(() -> questionService.createQuestion(99L, 10L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createAnswer_byStaff_automaticallyMarksOfficial() {
        when(questionRepository.findById(100L)).thenReturn(Optional.of(sampleQuestion));
        when(userRepository.findById(20L)).thenReturn(Optional.of(staffUser));
        when(answerRepository.save(any(ProductAnswer.class))).thenReturn(sampleAnswer);

        CreateAnswerRequest request = new CreateAnswerRequest("Yes, it supports up to 32GB RAM.");
        AnswerResponse response = questionService.createAnswer(100L, 20L, request);

        assertThat(response).isNotNull();
        assertThat(response.isOfficial()).isTrue();
        assertThat(response.userName()).isEqualTo("Staff Member");
    }

    @Test
    void markAnswerOfficial_byCustomer_throwsAccessDenied() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(customerUser));

        assertThatThrownBy(() -> questionService.markAnswerOfficial(200L, 10L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void markAnswerOfficial_byStaff_success() {
        when(userRepository.findById(20L)).thenReturn(Optional.of(staffUser));
        when(answerRepository.findById(200L)).thenReturn(Optional.of(sampleAnswer));
        when(answerRepository.save(any(ProductAnswer.class))).thenReturn(sampleAnswer);

        AnswerResponse response = questionService.markAnswerOfficial(200L, 20L);

        assertThat(response).isNotNull();
        assertThat(response.isOfficial()).isTrue();
    }

    @Test
    void getQuestionsByProduct_returnsOnlyVisibleQuestionsAndAnswers() {
        when(productRepository.existsById(1L)).thenReturn(true);
        Page<ProductQuestion> page = new PageImpl<>(List.of(sampleQuestion), PageRequest.of(0, 10), 1);
        when(questionRepository.findByProductIdAndStatus(1L, ModerationStatus.VISIBLE, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))))
                .thenReturn(page);
        when(answerRepository.findByQuestionIdInAndStatusOrderByIsOfficialDescCreatedAtAsc(List.of(100L), ModerationStatus.VISIBLE))
                .thenReturn(List.of(sampleAnswer));

        Page<QuestionResponse> result = questionService.getQuestionsByProduct(1L, null, PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).answers()).hasSize(1);
        assertThat(result.getContent().get(0).answers().get(0).isOfficial()).isTrue();
    }
}
