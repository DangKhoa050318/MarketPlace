package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.common.ApiResponse;
import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateReviewRequest;
import com.training.marketplace.dto.request.LoginRequest;
import com.training.marketplace.dto.request.UpdateReviewRequest;
import com.training.marketplace.dto.response.AuthResponse;
import com.training.marketplace.dto.response.ProductReviewResponse;
import com.training.marketplace.dto.response.RatingSummaryResponse;
import com.training.marketplace.dto.response.ReviewEligibilityResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductVariant;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.Role;
import com.training.marketplace.repository.OrderItemRepository;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductReviewRepository;
import com.training.marketplace.repository.ProductVariantRepository;
import com.training.marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReviewIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Product testProduct;
    private ProductVariant testVariant;
    private String userToken;

    @BeforeEach
    void setUpData() {
        reviewRepository.deleteAll();
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        variantRepository.deleteAll();
        productRepository.deleteAll();

        String suffix = UUID.randomUUID().toString().substring(0, 6);
        testUser = userRepository.save(User.builder()
                .username("reviewer_" + suffix)
                .email("reviewer_" + suffix + "@test.com")
                .password(passwordEncoder.encode("admin123"))
                .role(Role.CUSTOMER)
                .active(true)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Mechanical Keyboard Pro " + suffix)
                .slug("mech-key-pro-" + suffix)
                .active(true)
                .build());

        testVariant = variantRepository.save(ProductVariant.builder()
                .productId(testProduct.getId())
                .sku("SKU-KEY-" + suffix)
                .variantName("Blue Switches")
                .price(new BigDecimal("99.99"))
                .active(true)
                .build());

        LoginRequest loginReq = new LoginRequest(testUser.getUsername(), "admin123");
        ResponseEntity<AuthResponse> loginResp = restTemplate.postForEntity(
                "/api/v1/auth/login",
                loginReq,
                AuthResponse.class
        );
        assertThat(loginResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        userToken = loginResp.getBody().accessToken();
    }

    @Test
    @DisplayName("REQ-STP-T-102 & 104: Full E2E Review Flow (Eligibility, Verified Purchase, Create, Summary, Update, Soft Delete)")
    void testFullReviewFlow() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(userToken);

        // 1. Check eligibility before purchase -> eligible = true, isVerifiedPurchase = false
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<ApiResponse<ReviewEligibilityResponse>> eligBeforeResp = restTemplate.exchange(
                "/api/v1/products/" + testProduct.getId() + "/reviews/eligibility",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(eligBeforeResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(eligBeforeResp.getBody().getData().isEligible()).isTrue();
        assertThat(eligBeforeResp.getBody().getData().isVerifiedPurchase()).isFalse();

        // 2. Create a completed order for verified purchase check
        Order order = orderRepository.save(Order.builder()
                .user(testUser)
                .status(OrderStatus.DELIVERED)
                .totalAmount(new BigDecimal("99.99"))
                .build());

        orderItemRepository.save(OrderItem.builder()
                .order(order)
                .variantId(testVariant.getId())
                .productName(testProduct.getName())
                .variantName(testVariant.getVariantName())
                .sku(testVariant.getSku())
                .quantity(1)
                .unitPrice(new BigDecimal("99.99"))
                .subtotal(new BigDecimal("99.99"))
                .build());

        // 3. Check eligibility after purchase -> isVerifiedPurchase = true
        ResponseEntity<ApiResponse<ReviewEligibilityResponse>> eligAfterResp = restTemplate.exchange(
                "/api/v1/products/" + testProduct.getId() + "/reviews/eligibility",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(eligAfterResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(eligAfterResp.getBody().getData().isVerifiedPurchase()).isTrue();

        // 4. Submit a 5-star Review
        CreateReviewRequest createReq = new CreateReviewRequest(5, "Amazing Keyboard!", "The key feel and sound are absolutely top notch.");
        HttpEntity<CreateReviewRequest> createEntity = new HttpEntity<>(createReq, headers);
        ResponseEntity<ApiResponse<ProductReviewResponse>> createResp = restTemplate.exchange(
                "/api/v1/products/" + testProduct.getId() + "/reviews",
                HttpMethod.POST,
                createEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ProductReviewResponse createdReview = createResp.getBody().getData();
        assertThat(createdReview.getRating()).isEqualTo(5);
        assertThat(createdReview.getIsVerifiedPurchase()).isTrue();

        // 5. Get Rating Summary -> average 5.0, total 1
        ResponseEntity<ApiResponse<RatingSummaryResponse>> summaryResp = restTemplate.exchange(
                "/api/v1/products/" + testProduct.getId() + "/reviews/summary",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(summaryResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(summaryResp.getBody().getData().getAverageRating()).isEqualTo(5.0);
        assertThat(summaryResp.getBody().getData().getTotalReviews()).isEqualTo(1L);

        // 6. Attempt Duplicate Review -> 409 Conflict / 400 Bad Request
        ResponseEntity<ApiResponse<ProductReviewResponse>> dupResp = restTemplate.exchange(
                "/api/v1/products/" + testProduct.getId() + "/reviews",
                HttpMethod.POST,
                createEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(dupResp.getStatusCode()).isIn(HttpStatus.CONFLICT, HttpStatus.BAD_REQUEST);

        // 7. Update Review -> 4 stars
        UpdateReviewRequest updateReq = new UpdateReviewRequest(4, "Great Keyboard (Updated)", "Updated review: very good overall after 2 weeks.");
        HttpEntity<UpdateReviewRequest> updateEntity = new HttpEntity<>(updateReq, headers);
        ResponseEntity<ApiResponse<ProductReviewResponse>> updateResp = restTemplate.exchange(
                "/api/v1/reviews/" + createdReview.getId(),
                HttpMethod.PUT,
                updateEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(updateResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updateResp.getBody().getData().getRating()).isEqualTo(4);
        assertThat(updateResp.getBody().getData().getIsEdited()).isTrue();

        // 8. Soft Delete Review
        ResponseEntity<ApiResponse<Void>> deleteResp = restTemplate.exchange(
                "/api/v1/reviews/" + createdReview.getId(),
                HttpMethod.DELETE,
                requestEntity,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(deleteResp.getStatusCode()).isEqualTo(HttpStatus.OK);

        // 9. Summary after soft delete -> total 0
        ResponseEntity<ApiResponse<RatingSummaryResponse>> summaryAfterDelResp = restTemplate.exchange(
                "/api/v1/products/" + testProduct.getId() + "/reviews/summary",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );
        assertThat(summaryAfterDelResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(summaryAfterDelResp.getBody().getData().getTotalReviews()).isEqualTo(0L);
    }
}
