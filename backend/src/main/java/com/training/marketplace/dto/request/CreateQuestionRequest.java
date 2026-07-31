package com.training.marketplace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQuestionRequest(
        @NotBlank(message = "Question content is required")
        @Size(max = 1000, message = "Content cannot exceed 1000 characters")
        String content
) {}
