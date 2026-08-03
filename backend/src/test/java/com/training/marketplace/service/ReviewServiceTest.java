package com.training.marketplace.service;

import com.training.marketplace.common.PageResponse;
import com.training.marketplace.dto.request.CreateReviewRequest;
import com.training.marketplace.dto.request.UpdateReviewRequest;
import com.training.marketplace.dto.response.ProductReviewResponse;
import com.training.marketplace.dto.response.RatingSummaryResponse;
import com.training.marketplace.dto.response.ReviewEligibilityResponse;
import com.training.marketplace.entity.OrderItem;
import com.training.marketplace.entity.Product;
import com.training.marketplace.entity.ProductReview;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.Role;
import com.training.marketplace.enums.ReviewStatus;
import com.training.marketplace.exception.DuplicateResourceException;
import com.training.marketplace.exception.ForbiddenException;
import com.training.marketplace.repository.OrderItemRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.ProductReviewRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.impl.ReviewServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ProductReviewRepository reviewRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    private User sampleUser;
    private Product sampleProduct;
    private ProductReview sampleReview;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .username("customer1")
                .email("customer1@example.com")
                .role(Role.CUSTOMER)
                .build();
        sampleUser.setId(10L);

        sampleProduct = Product.builder()
                .name("Keyboard Master")
                .slug("keyboard-master")
                .build();
        sampleProduct.setId(20L);

        sampleReview = ProductReview.builder()
                .user(sampleUser)
                .product(sampleProduct)
                .rating(5)
                .title("Excellent product")
                .content("Build quality is superb, highly recommended.")
                .status(ReviewStatus.APPROVED)
                .isVerifiedPurchase(true)
                .isEdited(false)
                .build();
        sampleReview.setId(100L);
    }

    @Test
    @DisplayName("REQ-STP-T-101: Should create review successfully with verified purchase status")
    void testCreateReview_VerifiedPurchase_Success() {
        CreateReviewRequest req = new CreateReviewRequest(5, "Excellent product", "Build quality is superb, highly recommended.", null);
        OrderItem item = OrderItem.builder().id(50L).build();

        when(productRepository.findById(20L)).thenReturn(Optional.of(sampleProduct));
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(sampleUser));
        when(orderItemRepository.findEligibleOrderItemsForReview(10L, 20L)).thenReturn(List.of(item));
        when(reviewRepository.existsByUserIdAndOrderItemIdAndDeletedAtIsNull(10L, 50L)).thenReturn(false);
        when(reviewRepository.save(any(ProductReview.class))).thenReturn(sampleReview);

        ProductReviewResponse res = reviewService.createReview(20L, "customer1", req);

        assertThat(res).isNotNull();
        assertThat(res.getRating()).isEqualTo(5);
        assertThat(res.getIsVerifiedPurchase()).isTrue();
        verify(reviewRepository).save(any(ProductReview.class));
    }

    @Test
    @DisplayName("REQ-STP-T-102: Verified purchase is derived from eligible order items")
    void testCreateReview_NoEligibleOrder_CannotBecomeVerified() {
        CreateReviewRequest req = new CreateReviewRequest(
                4, "Independent review", "No purchase is associated with this review.", null);
        when(productRepository.findById(20L)).thenReturn(Optional.of(sampleProduct));
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(sampleUser));
        when(orderItemRepository.findEligibleOrderItemsForReview(10L, 20L)).thenReturn(Collections.emptyList());
        when(reviewRepository.save(any(ProductReview.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductReviewResponse response = reviewService.createReview(20L, "customer1", req);

        ArgumentCaptor<ProductReview> reviewCaptor = ArgumentCaptor.forClass(ProductReview.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getIsVerifiedPurchase()).isFalse();
        assertThat(reviewCaptor.getValue().getOrderItem()).isNull();
        assertThat(response.getIsVerifiedPurchase()).isFalse();
    }

    @Test
    @DisplayName("REQ-STP-T-103: Should reject duplicate review submission from same order item")
    void testCreateReview_DuplicateReview_ThrowsException() {
        CreateReviewRequest req = new CreateReviewRequest(4, "Second review", "Trying to write a second review for product.", null, 50L);
        OrderItem item = OrderItem.builder().id(50L).build();

        when(productRepository.findById(20L)).thenReturn(Optional.of(sampleProduct));
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(sampleUser));
        when(orderItemRepository.findById(50L)).thenReturn(Optional.of(item));
        when(reviewRepository.existsByUserIdAndOrderItemIdAndDeletedAtIsNull(10L, 50L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(20L, "customer1", req))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("REQ-STP-T-104: Should calculate rating summary accurately")
    void testGetRatingSummary_AccurateAggregation() {
        when(productRepository.findById(20L)).thenReturn(Optional.of(sampleProduct));

        List<Object[]> mockCounts = List.of(
                new Object[]{5, 10L},
                new Object[]{4, 5L},
                new Object[]{3, 1L}
        );
        when(reviewRepository.countReviewsGroupByRating(20L)).thenReturn(mockCounts);

        RatingSummaryResponse summary = reviewService.getRatingSummary(20L);

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalReviews()).isEqualTo(16L);
        // (5*10 + 4*5 + 3*1) / 16 = (50 + 20 + 3) / 16 = 73 / 16 = 4.5625 -> rounded to 4.6
        assertThat(summary.getAverageRating()).isEqualTo(4.6);
        assertThat(summary.getStarCounts().get(5)).isEqualTo(10L);
        assertThat(summary.getStarCounts().get(4)).isEqualTo(5L);
        assertThat(summary.getStarCounts().get(1)).isEqualTo(0L);
    }

    @Test
    @DisplayName("REQ-STP-T-101: Should throw ForbiddenException when updating review of another user")
    void testUpdateReview_NonOwner_ThrowsForbidden() {
        User anotherUser = User.builder().username("otheruser").role(Role.CUSTOMER).build();
        anotherUser.setId(99L);

        UpdateReviewRequest req = new UpdateReviewRequest(3, "Modified title", "Modified content body text", null);

        when(reviewRepository.findById(100L)).thenReturn(Optional.of(sampleReview));
        when(userRepository.findByUsername("otheruser")).thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> reviewService.updateReview(100L, "otheruser", req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("REQ-STP-T-103: Owner can update the existing review and it is marked edited")
    void testUpdateReview_Owner_UpdatesAccordingToPolicy() {
        UpdateReviewRequest req = new UpdateReviewRequest(
                4, "Updated title", "Updated content with enough detail.", null);
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(sampleReview));
        when(userRepository.findByUsername("customer1")).thenReturn(Optional.of(sampleUser));
        when(reviewRepository.save(sampleReview)).thenReturn(sampleReview);

        ProductReviewResponse response = reviewService.updateReview(100L, "customer1", req);

        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getTitle()).isEqualTo("Updated title");
        assertThat(response.getIsEdited()).isTrue();
    }

    @Test
    @DisplayName("REQ-STP-T-104: Hidden and approved moderation states are persisted")
    void testAdminStatus_HideAndShowReview() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(sampleReview));
        when(reviewRepository.save(sampleReview)).thenReturn(sampleReview);

        ProductReviewResponse hidden = reviewService.adminUpdateStatus(100L, ReviewStatus.HIDDEN);
        ProductReviewResponse shown = reviewService.adminUpdateStatus(100L, ReviewStatus.APPROVED);

        assertThat(hidden.getStatus()).isEqualTo(ReviewStatus.HIDDEN);
        assertThat(shown.getStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(sampleReview.getDeletedAt()).isNull();
    }
}
