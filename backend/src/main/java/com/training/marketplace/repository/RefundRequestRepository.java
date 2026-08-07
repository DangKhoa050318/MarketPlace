package com.training.marketplace.repository;

import com.training.marketplace.entity.RefundRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefundRequestRepository extends JpaRepository<RefundRequest, Long> {
    Optional<RefundRequest> findByIdempotencyKey(String idempotencyKey);

    Optional<RefundRequest> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
}
