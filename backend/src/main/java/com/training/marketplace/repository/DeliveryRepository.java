package com.training.marketplace.repository;

import com.training.marketplace.entity.Delivery;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DeliveryRepository extends JpaRepository<Delivery, Long> {

    Optional<Delivery> findByOrderId(Long orderId);

    @Query("""
            select d from Delivery d
            where lower(d.carrier) = lower(:carrier)
              and upper(d.trackingCode) = upper(:trackingCode)
            """)
    Optional<Delivery> findByNormalizedTracking(
            @Param("carrier") String carrier,
            @Param("trackingCode") String trackingCode);

    @Query("select d.orderId from Delivery d where d.id = :id")
    Optional<Long> findOrderIdById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Delivery d where d.id = :id")
    Optional<Delivery> findByIdForUpdate(@Param("id") Long id);
}
