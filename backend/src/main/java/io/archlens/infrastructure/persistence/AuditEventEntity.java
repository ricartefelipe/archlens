package io.archlens.infrastructure.persistence;

import io.archlens.domain.AuditAction;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEventEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    @Enumerated(EnumType.STRING)
    public AuditAction action;
    public String actor;
    @Lob
    public String details;
    @Column(name = "occurred_at")
    public Instant occurredAt;

    public static List<AuditEventEntity> listByOrg(UUID orgId, int limit) {
        return find("organizationId = ?1 order by occurredAt desc", orgId).page(0, limit).list();
    }
}
