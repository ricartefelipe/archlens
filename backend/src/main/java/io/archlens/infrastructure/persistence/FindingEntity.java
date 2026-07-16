package io.archlens.infrastructure.persistence;

import io.archlens.domain.DimensionKey;
import io.archlens.domain.FindingSeverity;
import io.archlens.domain.FindingStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "finding")
public class FindingEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    @Column(name = "rule_id")
    public String ruleId;
    public String title;
    @Lob
    public String description;
    @Enumerated(EnumType.STRING)
    public FindingSeverity severity;
    @Enumerated(EnumType.STRING)
    public FindingStatus status;
    @Column(name = "evidence_ids")
    public String evidenceIds;
    @Enumerated(EnumType.STRING)
    @Column(name = "dimension_key")
    public DimensionKey dimensionKey;
    @Column(name = "review_note")
    public String reviewNote;
    @Column(name = "reviewed_by")
    public String reviewedBy;
    @Column(name = "reviewed_at")
    public Instant reviewedAt;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public static List<FindingEntity> listByAssessment(UUID assessmentId) {
        return list("assessmentId = ?1 order by severity, title", assessmentId);
    }

    public static FindingEntity findByAssessmentAndId(UUID assessmentId, UUID id) {
        return find("assessmentId = ?1 and id = ?2", assessmentId, id).firstResult();
    }
}
