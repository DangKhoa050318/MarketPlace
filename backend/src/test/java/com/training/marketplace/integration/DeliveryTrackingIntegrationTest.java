package com.training.marketplace.integration;

import com.training.marketplace.BaseIntegrationTest;
import com.training.marketplace.dto.request.CreateDeliveryRequest;
import com.training.marketplace.entity.Delivery;
import com.training.marketplace.entity.Order;
import com.training.marketplace.entity.User;
import com.training.marketplace.enums.DeliveryStatus;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.Role;
import com.training.marketplace.repository.DeliveryEventRepository;
import com.training.marketplace.repository.DeliveryRepository;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DeliveryTrackingIntegrationTest extends BaseIntegrationTest {

    @Autowired private DeliveryService deliveryService;
    @Autowired private DeliveryRepository deliveryRepository;
    @Autowired private DeliveryEventRepository deliveryEventRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    @Test
    void normalizedTrackingQuery_matchesCarrierLowerAndTrackingCodeUpperExpressions() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User customer = userRepository.save(User.builder()
                .username("tracking-query-" + suffix)
                .email("tracking-query-" + suffix + "@example.com")
                .password("encoded")
                .role(Role.CUSTOMER)
                .active(true)
                .build());
        Order order = orderRepository.save(Order.builder()
                .user(customer)
                .warehouseId(1L)
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("100.00"))
                .shippingAddress("Bangkok")
                .build());
        Delivery delivery = deliveryRepository.saveAndFlush(Delivery.builder()
                .orderId(order.getId())
                .carrier("GhN")
                .trackingCode("AbC-" + suffix)
                .status(DeliveryStatus.PENDING)
                .estimatedDelivery(LocalDate.now().plusDays(2))
                .createdBy(customer.getId())
                .build());

        assertThat(deliveryRepository.findByNormalizedTracking(
                "gHn", "aBc-" + suffix))
                .contains(delivery);
    }

    @Test
    void concurrentCreate_producesOneDeliveryAndOneInitialEvent() throws InterruptedException {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        User customer = userRepository.save(User.builder()
                .username("delivery-customer-" + suffix)
                .email("delivery-customer-" + suffix + "@example.com")
                .password("encoded")
                .role(Role.CUSTOMER)
                .active(true)
                .build());
        User staff = userRepository.save(User.builder()
                .username("delivery-staff-" + suffix)
                .email("delivery-staff-" + suffix + "@example.com")
                .password("encoded")
                .role(Role.STAFF)
                .active(true)
                .build());
        Order order = orderRepository.save(Order.builder()
                .user(customer)
                .warehouseId(1L)
                .status(OrderStatus.SHIPPED)
                .totalAmount(new BigDecimal("100.00"))
                .shippingAddress("Bangkok")
                .build());

        CreateDeliveryRequest request = new CreateDeliveryRequest(
                "GHN", "CONCURRENT-" + suffix, LocalDate.now().plusDays(2));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    deliveryService.create(order.getId(), staff.getId(), request);
                    success.incrementAndGet();
                } catch (Exception ex) {
                    failure.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        start.countDown();
        done.await();
        executor.shutdown();

        Delivery delivery = deliveryRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(success.get()).isEqualTo(1);
        assertThat(failure.get()).isEqualTo(1);
        assertThat(deliveryEventRepository.findByDeliveryIdOrderByOccurredAtAscIdAsc(delivery.getId()))
                .hasSize(1);
    }
}
