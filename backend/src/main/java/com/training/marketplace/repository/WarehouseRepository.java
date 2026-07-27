package com.training.marketplace.repository;

import com.training.marketplace.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByCode(String code);

    boolean existsByCode(String code);
}
