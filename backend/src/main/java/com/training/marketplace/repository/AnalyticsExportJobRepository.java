package com.training.marketplace.repository;

import com.training.marketplace.entity.AnalyticsExportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AnalyticsExportJobRepository extends JpaRepository<AnalyticsExportJob, Long> {

    Optional<AnalyticsExportJob> findByPublicId(UUID publicId);
}
