package io.archlens.infrastructure.persistence;

import io.archlens.domain.AssessmentStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assessment")
public class AssessmentEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    public String name;
    public String description;
    @Enumerated(EnumType.STRING)
    public AssessmentStatus status;
    @Column(name = "project_root_name")
    public String projectRootName;
    @Column(name = "created_by")
    public String createdBy;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public static AssessmentEntity findByOrgAndId(UUID orgId, UUID id) {
        return find("organizationId = ?1 and id = ?2", orgId, id).firstResult();
    }

    public static List<AssessmentEntity> listByOrg(UUID orgId) {
        return list("organizationId = ?1 order by createdAt desc", orgId);
    }
}
