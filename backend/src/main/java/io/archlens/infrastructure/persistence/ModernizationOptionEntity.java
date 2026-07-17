package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "modernization_option")
public class ModernizationOptionEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    public String title;
    @Column(columnDefinition = "text")
    public String description;
    public String effort;
    public String impact;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public static List<ModernizationOptionEntity> listByOrgAndAssessment(UUID orgId, UUID assessmentId) {
        return list("organizationId = ?1 and assessmentId = ?2 order by createdAt", orgId, assessmentId);
    }

    public static ModernizationOptionEntity findByOrgAssessmentAndId(UUID orgId, UUID assessmentId, UUID id) {
        return find("organizationId = ?1 and assessmentId = ?2 and id = ?3", orgId, assessmentId, id)
                .firstResult();
    }
}
