package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_dependency")
public class InventoryDependencyEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    public String coordinates;
public String scope;
@Column(name = "source_path")
public String sourcePath;

    public static List<InventoryDependencyEntity> listByAssessment(UUID assessmentId) {
        return list("assessmentId = ?1", assessmentId);
    }

    public static long deleteByAssessment(UUID assessmentId) {
        return delete("assessmentId = ?1", assessmentId);
    }
}
