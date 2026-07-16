package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_type")
public class InventoryTypeEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    public String fqn;
public String kind;
@Column(name = "package_name")
public String packageName;
public String path;
@Column(name = "line_count")
public int lineCount;
public String annotations;

    public static List<InventoryTypeEntity> listByAssessment(UUID assessmentId) {
        return list("assessmentId = ?1", assessmentId);
    }

    public static long deleteByAssessment(UUID assessmentId) {
        return delete("assessmentId = ?1", assessmentId);
    }
}
