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
    @DisplayName("REQ-STP-T-101: blank and short review content is rejected")
    void rejectsInvalidContent() {
        assertThat(propertyViolations(new CreateReviewRequest(5, "Good", " ", null), "content")).isNotEmpty();
        assertThat(propertyViolations(new CreateReviewRequest(5, "Good", "too short", null), "content")).isNotEmpty();
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
