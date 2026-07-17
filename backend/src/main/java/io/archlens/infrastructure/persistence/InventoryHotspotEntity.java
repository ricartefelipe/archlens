package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_hotspot")
public class InventoryHotspotEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    public String path;
@Column(name = "line_count")
public int lineCount;
public String reason;
@Column(name = "evidence_id")
public String evidenceId;

    public static List<InventoryHotspotEntity> listByAssessment(UUID assessmentId) {
        return list("assessmentId = ?1", assessmentId);
    }

    public static long deleteByAssessment(UUID assessmentId) {
        return delete("assessmentId = ?1", assessmentId);
    }
}
