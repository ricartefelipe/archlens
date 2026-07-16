package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "roadmap_item")
public class RoadmapItemEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    public String title;
    @Lob
    public String description;
    public String phase;
    @Column(name = "sort_order")
    public int sortOrder;
    @Column(name = "modernization_option_id", columnDefinition = "uuid")
    public UUID modernizationOptionId;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public static List<RoadmapItemEntity> listByAssessment(UUID assessmentId) {
        return list("assessmentId = ?1 order by sortOrder", assessmentId);
    }
}
