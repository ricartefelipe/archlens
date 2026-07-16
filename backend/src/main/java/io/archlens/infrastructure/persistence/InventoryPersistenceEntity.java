package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_persistence")
public class InventoryPersistenceEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    public String technology;
public String name;
@Column(name = "source_path")
public String sourcePath;
@Column(name = "evidence_id")
public String evidenceId;

    public static List<InventoryPersistenceEntity> listByAssessment(UUID assessmentId) {
        return list("assessmentId = ?1", assessmentId);
    }

    public static long deleteByAssessment(UUID assessmentId) {
        return delete("assessmentId = ?1", assessmentId);
    }
}
