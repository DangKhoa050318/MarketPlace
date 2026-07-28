package com.training.marketplace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Minimal audit trail for privileged admin actions (cross-feature convention: "mọi thao tác quản
 * trị quan trọng phải có audit log"). Writes structured entries — actor, action, target, details —
 * to a dedicated {@code AUDIT} logger. Persisting to an audit table is a shared follow-up: it needs
 * a coordinated migration and a schema agreed across features, so it is intentionally out of scope
 * for FEATURE-STP-02.
 */
@Service
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    public void record(String actor, String action, String targetType, Object targetId, String details) {
        AUDIT.info("audit action={} target={}#{} actor={} details=[{}]",
                action, targetType, targetId, (actor == null || actor.isBlank()) ? "system" : actor, details);
    }
}
