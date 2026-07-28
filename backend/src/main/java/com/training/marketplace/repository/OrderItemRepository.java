package com.training.marketplace.repository;

import com.training.marketplace.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi JOIN oi.order o JOIN ProductVariant pv ON oi.variantId = pv.id " +
           "WHERE o.user.id = :userId AND pv.productId = :productId AND o.status != com.training.marketplace.enums.OrderStatus.CANCELLED")
    List<OrderItem> findEligibleOrderItemsForReview(@Param("userId") Long userId, @Param("productId") Long productId);
}
