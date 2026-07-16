package io.archlens.infrastructure.persistence;

import io.archlens.domain.DimensionKey;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "dimension_score")
public class DimensionScoreEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    @Enumerated(EnumType.STRING)
    @Column(name = "dimension_key")
    public DimensionKey dimensionKey;
    public BigDecimal score;
    public String rationale;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public static List<DimensionScoreEntity> listByAssessment(UUID assessmentId) {
        return list("assessmentId = ?1", assessmentId);
    }
}
