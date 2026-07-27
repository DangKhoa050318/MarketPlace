package com.training.marketplace.repository;

import com.training.marketplace.entity.StockMovementItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockMovementItemRepository extends JpaRepository<StockMovementItem, Long> {

    List<StockMovementItem> findByMovementId(Long movementId);
}
