package com.training.marketplace.repository;

import com.training.marketplace.entity.ReturnRequest;
import com.training.marketplace.enums.ReturnRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    Optional<ReturnRequest> findByOrderIdAndStatusIn(Long orderId, Collection<ReturnRequestStatus> statuses);

    List<ReturnRequest> findAllByStatusInOrderByCreatedAtDesc(Collection<ReturnRequestStatus> statuses);
}
