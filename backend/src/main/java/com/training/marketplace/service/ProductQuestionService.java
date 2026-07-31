package com.training.marketplace.service;

import com.training.marketplace.dto.request.CreateAnswerRequest;
import com.training.marketplace.dto.request.CreateQuestionRequest;
import com.training.marketplace.dto.response.AnswerResponse;
import com.training.marketplace.dto.response.QuestionResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductQuestionService {

    QuestionResponse createQuestion(Long productId, Long userId, CreateQuestionRequest request);

    AnswerResponse createAnswer(Long questionId, Long userId, CreateAnswerRequest request);

    AnswerResponse markAnswerOfficial(Long answerId, Long currentUserId);

    Page<QuestionResponse> getQuestionsByProduct(Long productId, Long currentUserId, Pageable pageable);
}
