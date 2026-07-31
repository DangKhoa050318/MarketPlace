package com.training.marketplace.repository;

import com.training.marketplace.entity.ModerationAuditLog;
import com.training.marketplace.enums.ContentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModerationAuditLogRepository extends JpaRepository<ModerationAuditLog, Long> {

    List<ModerationAuditLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(ContentType targetType, Long targetId);
}
