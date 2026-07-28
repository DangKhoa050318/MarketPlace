package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.dto.response.CartItemResponse;
import com.training.marketplace.dto.response.CartResponse;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.PromotionCode;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.DiscountType;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.PromotionScopeType;
import com.training.marketplace.enums.Role;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.PromotionCodeRepository;
import com.training.marketplace.repository.PromotionRedemptionRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.AppliedCoupon;
import com.training.marketplace.service.PromotionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REQ-STP-T-304: coupon usage limits under concurrency, guarded by the pessimistic lock on the
 * coupon row. Two scenarios: the total {@code usage_limit} (distinct users) and the per-user limit
 * (same user), both must never be exceeded when many requests race.
 */
class ConcurrentCouponUsageIntegrationTest extends BaseIntegrationTest {

    @Autowired private PromotionCodeRepository promotionCodeRepository;
    @Autowired private PromotionRedemptionRepository redemptionRepository;
    @Autowired private PromotionService promotionService;
    @Autowired private UserRepository userRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private PlatformTransactionManager txManager;

    private CartResponse cart(long userId) {
        return new CartResponse(userId,
                List.of(new CartItemResponse(10L, "SKU10", "Product", "Variant",
                        new BigDecimal("500.00"), 1, new BigDecimal("500.00"), null)),
                new BigDecimal("500.00"), 1);
    }

    @Test
    void testConcurrentConsume_NeverExceedsUsageLimit() throws InterruptedException {
        PromotionCode coupon = promotionCodeRepository.save(PromotionCode.builder()
                .code("CONCUR10").discountType(DiscountType.FIXED).discountValue(new BigDecimal("10.00"))
                .minOrderAmount(BigDecimal.ZERO).scopeType(PromotionScopeType.CART)
                .usageLimit(10).usedCount(0).active(true).build());

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final long userId = i + 1; // distinct users so only the total usage limit binds
            executor.submit(() -> {
                try {
                    startLatch.await();
                    promotionService.consume("CONCUR10", userId, cart(userId));
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        PromotionCode reloaded = promotionCodeRepository.findById(coupon.getId()).orElseThrow();
        assertThat(successCount.get()).isEqualTo(10);
        assertThat(failureCount.get()).isEqualTo(10);
        assertThat(reloaded.getUsedCount()).isEqualTo(10);
    }

    @Test
    void testConcurrentConsume_SameUser_NeverExceedsPerUserLimit() throws InterruptedException {
        PromotionCode coupon = promotionCodeRepository.save(PromotionCode.builder()
                .code("PERUSER1").discountType(DiscountType.FIXED).discountValue(new BigDecimal("10.00"))
                .minOrderAmount(BigDecimal.ZERO).scopeType(PromotionScopeType.CART)
                .perUserLimit(1).usedCount(0).active(true).build());

        User user = userRepository.save(User.builder()
                .username("peruser").email("peruser@example.com").password("x")
                .role(Role.CUSTOMER).active(true).build());
        final Long userId = user.getId();

        // Each thread does consume + create order + record redemption atomically, mirroring the real
        // order transaction (the coupon-row lock is held across all three until commit).
        TransactionTemplate tx = new TransactionTemplate(txManager);
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    tx.executeWithoutResult(status -> {
                        AppliedCoupon applied = promotionService.consume("PERUSER1", userId, cart(userId));
                        Order order = orderRepository.save(Order.builder()
                                .user(user).status(OrderStatus.PENDING)
                                .totalAmount(new BigDecimal("490.00")).shippingAddress("addr").build());
                        promotionService.recordRedemption(
                                applied.promotionCodeId(), userId, order.getId(), applied.discountAmount());
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        finishLatch.await();
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(9);
        assertThat(redemptionRepository.countByPromotionCodeIdAndUserId(coupon.getId(), userId)).isEqualTo(1);
    }
}
