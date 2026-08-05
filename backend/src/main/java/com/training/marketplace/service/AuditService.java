package com.training.marketplace.service;

import com.training.marketplace.entity.AuditLog;
import com.training.marketplace.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Persists privileged actions and mirrors them to the structured AUDIT logger. */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");
    private final AuditLogRepository auditLogRepository;

    public void record(String actor, String action, String targetType, Object targetId, String details) {
        String normalizedActor = actor == null || actor.isBlank() ? "system" : actor;
        AUDIT.info("audit action={} target={}#{} actor={} details=[{}]",
                action, targetType, targetId, normalizedActor, details);
        try {
            auditLogRepository.save(AuditLog.builder()
                    .actor(normalizedActor)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId == null ? null : targetId.toString())
                    .details(details)
                    .build());
        } catch (RuntimeException exception) {
            AUDIT.error("audit_persistence_failed action={} target={}#{} actor={}",
                    action, targetType, targetId, normalizedActor, exception);
        }
    }
}
