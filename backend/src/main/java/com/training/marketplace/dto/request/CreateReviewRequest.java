package com.training.marketplace.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequest(
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating cannot exceed 5")
        Integer rating,

        // Title and content are optional: a star-only rating is allowed (G2).
        @Size(max = 100, message = "Title cannot exceed 100 characters")
        String title,

        @Size(max = 1000, message = "Content cannot exceed 1000 characters")
        String content,

        @Size(max = 500, message = "Image URL cannot exceed 500 characters")
        String imageUrl,

        Long orderItemId
) {
    public CreateReviewRequest(Integer rating, String title, String content, String imageUrl) {
        this(rating, title, content, imageUrl, null);
    }
}
