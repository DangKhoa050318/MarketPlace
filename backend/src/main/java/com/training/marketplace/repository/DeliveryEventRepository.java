package com.training.marketplace.repository;

import com.training.marketplace.entity.DeliveryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryEventRepository extends JpaRepository<DeliveryEvent, Long> {

    List<DeliveryEvent> findByDeliveryIdOrderByOccurredAtAscIdAsc(Long deliveryId);

    Optional<DeliveryEvent> findByDeliveryIdAndRequestId(Long deliveryId, UUID requestId);
}
