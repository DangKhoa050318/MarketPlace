package com.training.marketplace.repository;

import com.training.marketplace.entity.Order;
import com.training.marketplace.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "items"})
    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "items"})
    @Override
    Page<Order> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "items"})
    @Override
    Optional<Order> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"user", "items"})
    @Query("select o from Order o where o.id = :id")
    Optional<Order> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"user", "items"})
    Page<Order> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "items"})
    @Query(
        value = "SELECT DISTINCT o FROM Order o LEFT JOIN o.items i WHERE o.user.id = :userId " +
                "AND (:status IS NULL OR o.status = :status) " +
                "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
                "AND (:search IS NULL OR LOWER(i.productName) LIKE :search OR CAST(o.id AS string) LIKE :search)",
        countQuery = "SELECT COUNT(DISTINCT o) FROM Order o LEFT JOIN o.items i WHERE o.user.id = :userId " +
                     "AND (:status IS NULL OR o.status = :status) " +
                     "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus) " +
                     "AND (:search IS NULL OR LOWER(i.productName) LIKE :search OR CAST(o.id AS string) LIKE :search)"
    )
    Page<Order> findFilteredOrders(
            @Param("userId") Long userId,
            @Param("status") OrderStatus status,
            @Param("paymentStatus") com.training.marketplace.enums.PaymentStatus paymentStatus,
            @Param("search") String search,
            Pageable pageable);

    long countByStatus(OrderStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != com.training.marketplace.enums.OrderStatus.CANCELLED")
    java.math.BigDecimal calculateTotalRevenue();
}
