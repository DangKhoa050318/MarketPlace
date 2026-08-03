package com.training.marketplace.repository;

import com.training.marketplace.entity.PaymentAttempt;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByEventId(String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PaymentAttempt p where p.eventId = :eventId")
    Optional<PaymentAttempt> findByEventIdForUpdate(@Param("eventId") String eventId);
}
