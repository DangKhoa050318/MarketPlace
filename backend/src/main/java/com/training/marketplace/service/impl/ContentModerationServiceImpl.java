package com.training.marketplace.service.impl;

import com.training.marketplace.dto.request.UpdateModerationStatusRequest;
import com.training.marketplace.dto.response.ModerationAuditLogResponse;
import com.training.marketplace.dto.response.ModerationItemResponse;
import com.training.marketplace.entity.ModerationAuditLog;
import com.training.marketplace.entity.ProductAnswer;
import com.training.marketplace.entity.ProductQuestion;
import com.training.marketplace.entity.ProductReview;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.ContentType;
import com.training.marketplace.enums.ModerationStatus;
import com.training.marketplace.enums.ReviewStatus;
import com.training.marketplace.exception.BadRequestException;
import com.training.marketplace.exception.ResourceNotFoundException;
import com.training.marketplace.repository.ModerationAuditLogRepository;
import com.training.marketplace.repository.ProductAnswerRepository;
import com.training.marketplace.repository.ProductQuestionRepository;
import com.training.marketplace.repository.ProductReviewRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.ContentModerationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ContentModerationServiceImpl implements ContentModerationService {

    private final ProductReviewRepository reviewRepository;
    private final ProductQuestionRepository questionRepository;
    private final ProductAnswerRepository answerRepository;
    private final ModerationAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ModerationItemResponse> getModerationQueue(
            ContentType targetType,
            ModerationStatus status,
            Long productId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {

        ContentType typeToQuery = targetType != null ? targetType : ContentType.QUESTION;

        if (typeToQuery == ContentType.QUESTION) {
            Specification<ProductQuestion> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (status != null) predicates.add(cb.equal(root.get("status"), status));
                if (productId != null) predicates.add(cb.equal(root.get("product").get("id"), productId));
                if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
                if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<ProductQuestion> questions = questionRepository.findAll(spec, pageable);
            List<ModerationItemResponse> content = questions.getContent().stream().map(q -> new ModerationItemResponse(
                    q.getId(),
                    ContentType.QUESTION,
                    q.getId(),
                    q.getProduct().getId(),
                    q.getProduct().getName(),
                    q.getUser().getId(),
                    q.getUser().getFullName() != null ? q.getUser().getFullName() : q.getUser().getUsername(),
                    q.getContent(),
                    q.getStatus(),
                    q.getCreatedAt()
            )).toList();
            return new PageImpl<>(content, pageable, questions.getTotalElements());
        }

        if (typeToQuery == ContentType.ANSWER) {
            Specification<ProductAnswer> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (status != null) predicates.add(cb.equal(root.get("status"), status));
                if (productId != null) predicates.add(cb.equal(root.get("question").get("product").get("id"), productId));
                if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
                if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Page<ProductAnswer> answers = answerRepository.findAll(spec, pageable);
            List<ModerationItemResponse> content = answers.getContent().stream().map(a -> new ModerationItemResponse(
                    a.getId(),
                    ContentType.ANSWER,
                    a.getId(),
                    a.getQuestion().getProduct().getId(),
                    a.getQuestion().getProduct().getName(),
                    a.getUser().getId(),
                    a.getUser().getFullName() != null ? a.getUser().getFullName() : a.getUser().getUsername(),
                    a.getContent(),
                    a.getStatus(),
                    a.getCreatedAt()
            )).toList();
            return new PageImpl<>(content, pageable, answers.getTotalElements());
        }

        // REVIEW
        ReviewStatus reviewStatus = null;
        if (status == ModerationStatus.VISIBLE) reviewStatus = ReviewStatus.APPROVED;
        else if (status == ModerationStatus.HIDDEN) reviewStatus = ReviewStatus.HIDDEN;
        else if (status == ModerationStatus.REJECTED) reviewStatus = ReviewStatus.DELETED;

        final ReviewStatus targetReviewStatus = reviewStatus;

        Specification<ProductReview> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNull(root.get("deletedAt")));
            if (targetReviewStatus != null) predicates.add(cb.equal(root.get("status"), targetReviewStatus));
            if (productId != null) predicates.add(cb.equal(root.get("product").get("id"), productId));
            if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate));
            if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ProductReview> reviews = reviewRepository.findAll(spec, pageable);
        List<ModerationItemResponse> content = reviews.getContent().stream().map(r -> {
            ModerationStatus mStatus = ModerationStatus.VISIBLE;
            if (r.getStatus() == ReviewStatus.HIDDEN) mStatus = ModerationStatus.HIDDEN;
            else if (r.getStatus() == ReviewStatus.DELETED) mStatus = ModerationStatus.REJECTED;

            return new ModerationItemResponse(
                    r.getId(),
                    ContentType.REVIEW,
                    r.getId(),
                    r.getProduct().getId(),
                    r.getProduct().getName(),
                    r.getUser().getId(),
                    r.getUser().getFullName() != null ? r.getUser().getFullName() : r.getUser().getUsername(),
                    r.getTitle() + " - " + r.getContent(),
                    mStatus,
                    r.getCreatedAt()
            );
        }).toList();

        return new PageImpl<>(content, pageable, reviews.getTotalElements());
    }

    @Override
    @Transactional
    public ModerationItemResponse updateStatus(Long moderatorId, UpdateModerationStatusRequest request) {
        User moderator = userRepository.findById(moderatorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", moderatorId));

        ModerationStatus newStatus = request.newStatus();
        if ((newStatus == ModerationStatus.HIDDEN || newStatus == ModerationStatus.REJECTED)
                && (request.reason() == null || request.reason().trim().isEmpty())) {
            throw new BadRequestException("Reason is required when hiding or rejecting content");
        }

        ContentType type = request.targetType();
        Long targetId = request.targetId();
        ModerationStatus oldStatus = null;
        ModerationItemResponse result = null;

        switch (type) {
            case QUESTION -> {
                ProductQuestion question = questionRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("ProductQuestion", targetId));
                oldStatus = question.getStatus();
                question.setStatus(newStatus);
                questionRepository.save(question);

                result = new ModerationItemResponse(
                        question.getId(), ContentType.QUESTION, question.getId(),
                        question.getProduct().getId(), question.getProduct().getName(),
                        question.getUser().getId(), question.getUser().getUsername(),
                        question.getContent(), newStatus, question.getCreatedAt()
                );
            }
            case ANSWER -> {
                ProductAnswer answer = answerRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("ProductAnswer", targetId));
                oldStatus = answer.getStatus();
                answer.setStatus(newStatus);
                answerRepository.save(answer);

                result = new ModerationItemResponse(
                        answer.getId(), ContentType.ANSWER, answer.getId(),
                        answer.getQuestion().getProduct().getId(), answer.getQuestion().getProduct().getName(),
                        answer.getUser().getId(), answer.getUser().getUsername(),
                        answer.getContent(), newStatus, answer.getCreatedAt()
                );
            }
            case REVIEW -> {
                ProductReview review = reviewRepository.findById(targetId)
                        .orElseThrow(() -> new ResourceNotFoundException("ProductReview", targetId));

                if (review.getStatus() == ReviewStatus.APPROVED) oldStatus = ModerationStatus.VISIBLE;
                else if (review.getStatus() == ReviewStatus.HIDDEN) oldStatus = ModerationStatus.HIDDEN;
                else if (review.getStatus() == ReviewStatus.DELETED) oldStatus = ModerationStatus.REJECTED;

                if (newStatus == ModerationStatus.VISIBLE) review.setStatus(ReviewStatus.APPROVED);
                else if (newStatus == ModerationStatus.HIDDEN) review.setStatus(ReviewStatus.HIDDEN);
                else if (newStatus == ModerationStatus.REJECTED) review.setStatus(ReviewStatus.DELETED);

                reviewRepository.save(review);

                result = new ModerationItemResponse(
                        review.getId(), ContentType.REVIEW, review.getId(),
                        review.getProduct().getId(), review.getProduct().getName(),
                        review.getUser().getId(), review.getUser().getUsername(),
                        review.getContent(), newStatus, review.getCreatedAt()
                );
            }
        }

        // Save audit log
        ModerationAuditLog auditLog = ModerationAuditLog.builder()
                .targetType(type)
                .targetId(targetId)
                .moderator(moderator)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .reason(request.reason() != null ? request.reason().trim() : null)
                .build();
        auditLogRepository.save(auditLog);

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationAuditLogResponse> getAuditLogs(ContentType targetType, Long targetId) {
        List<ModerationAuditLog> logs = auditLogRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(targetType, targetId);
        return logs.stream().map(l -> new ModerationAuditLogResponse(
                l.getId(),
                l.getTargetType(),
                l.getTargetId(),
                l.getModerator().getId(),
                l.getModerator().getFullName() != null ? l.getModerator().getFullName() : l.getModerator().getUsername(),
                l.getOldStatus(),
                l.getNewStatus(),
                l.getReason(),
                l.getCreatedAt()
        )).toList();
    }
}
