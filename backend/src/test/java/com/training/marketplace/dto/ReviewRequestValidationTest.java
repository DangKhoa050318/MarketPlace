package com.training.marketplace.dto;

import com.training.marketplace.dto.request.CreateReviewRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("REQ-STP-T-101: ratings 1 and 5 are valid boundaries")
    void acceptsBoundaryRatings() {
        assertThat(validator.validate(validRequest(1))).isEmpty();
        assertThat(validator.validate(validRequest(5))).isEmpty();
    }

    @Test
    @DisplayName("REQ-STP-T-101: ratings outside 1..5 are rejected")
    void rejectsOutOfRangeRatings() {
        assertThat(propertyViolations(validRequest(0), "rating")).isNotEmpty();
        assertThat(propertyViolations(validRequest(6), "rating")).isNotEmpty();
    }

    @Test
    @DisplayName("G2: a star-only review (no title/content) is valid")
    void acceptsStarOnlyReview() {
        assertThat(validator.validate(new CreateReviewRequest(5, null, null, null))).isEmpty();
        assertThat(validator.validate(new CreateReviewRequest(4, "", "", null))).isEmpty();
    }

    @Test
    @DisplayName("G2: over-length title/content is rejected")
    void rejectsOverLongText() {
        assertThat(propertyViolations(new CreateReviewRequest(5, "t".repeat(101), "ok", null), "title")).isNotEmpty();
        assertThat(propertyViolations(new CreateReviewRequest(5, "ok", "c".repeat(1001), null), "content")).isNotEmpty();
    }

    private CreateReviewRequest validRequest(int rating) {
        return new CreateReviewRequest(rating, "Useful review", "This review has enough detail.", null);
    }

    private Set<ConstraintViolation<CreateReviewRequest>> propertyViolations(
            CreateReviewRequest request, String property) {
        Set<ConstraintViolation<CreateReviewRequest>> violations = validator.validate(request);
        violations.removeIf(violation -> !violation.getPropertyPath().toString().equals(property));
        return violations;
    }
}
