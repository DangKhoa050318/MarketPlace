package com.training.marketplace.service;

import com.training.marketplace.dto.response.DashboardStatsResponse;
import com.training.marketplace.enums.OrderStatus;
import com.training.marketplace.enums.Role;
import com.training.marketplace.repository.OrderRepository;
import com.training.marketplace.repository.ProductRepository;
import com.training.marketplace.repository.UserRepository;
import com.training.marketplace.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void getDashboardStats_returnsAggregatedMetrics() {
        // Given
        when(orderRepository.count()).thenReturn(15L);
        when(orderRepository.calculateTotalRevenue()).thenReturn(new BigDecimal("1250.50"));
        when(orderRepository.countByStatus(OrderStatus.PENDING)).thenReturn(4L);
        when(orderRepository.countByStatus(OrderStatus.DELIVERED)).thenReturn(8L);
        when(productRepository.countByActiveTrue()).thenReturn(25L);
        when(userRepository.countByRole(Role.CUSTOMER)).thenReturn(10L);

        // When
        DashboardStatsResponse stats = orderService.getDashboardStats();

        // Then
        assertThat(stats.totalOrders()).isEqualTo(15L);
        assertThat(stats.totalRevenue()).isEqualTo(new BigDecimal("1250.50"));
        assertThat(stats.pendingOrders()).isEqualTo(4L);
        assertThat(stats.completedOrders()).isEqualTo(8L);
        assertThat(stats.totalProducts()).isEqualTo(25L);
        assertThat(stats.totalCustomers()).isEqualTo(10L);
    }
}
