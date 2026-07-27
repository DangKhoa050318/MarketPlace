package com.training.marketplace.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewEligibilityResponse {
    private boolean eligible;
    private boolean isVerifiedPurchase;
    private Long orderItemId;
    private ProductReviewResponse existingReview;
    private String message;
}
