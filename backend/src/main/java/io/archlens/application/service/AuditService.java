package io.archlens.application.service;

import io.archlens.domain.AuditAction;
import io.archlens.infrastructure.persistence.AuditEventEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class AuditService {

    @Transactional
    public void record(UUID orgId, UUID assessmentId, AuditAction action, String actor, String details) {
        AuditEventEntity e = new AuditEventEntity();
        e.id = UUID.randomUUID();
        e.organizationId = orgId;
        e.assessmentId = assessmentId;
        e.action = action;
        e.actor = actor;
        e.details = details;
        e.occurredAt = Instant.now();
        e.persist();
    }
}
