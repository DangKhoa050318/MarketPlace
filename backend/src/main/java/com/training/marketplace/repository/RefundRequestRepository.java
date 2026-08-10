package com.training.marketplace.repository;

import com.training.marketplace.entity.RefundRequest;
import com.training.marketplace.enums.RefundRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    Optional<RefundRequest> findByIdempotencyKey(String idempotencyKey);

    Optional<RefundRequest> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    @Query("select coalesce(sum(r.amount), 0) from RefundRequest r where r.order.id = :orderId and r.status = :status")
    BigDecimal sumAmountByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") RefundRequestStatus status);
}
