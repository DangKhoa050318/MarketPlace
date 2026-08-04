package com.training.marketplace.service;

import com.training.marketplace.entity.AuditLog;
import com.training.marketplace.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock private AuditLogRepository auditLogRepository;
    @InjectMocks private AuditService auditService;

    @Test
    void record_persistsActorActionTargetAndDetails() {
        auditService.record("admin@example.com", "ANALYTICS_EXPORT_CREATE", "AnalyticsExport", 42L,
                "type=OVERVIEW");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("admin@example.com");
        assertThat(captor.getValue().getAction()).isEqualTo("ANALYTICS_EXPORT_CREATE");
        assertThat(captor.getValue().getTargetId()).isEqualTo("42");
        assertThat(captor.getValue().getDetails()).isEqualTo("type=OVERVIEW");
    }
}
