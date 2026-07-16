package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "inventory_metrics")
public class InventoryMetricsEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    @Column(name = "file_count")
public int fileCount;
@Column(name = "java_file_count")
public int javaFileCount;
@Column(name = "line_count")
public int lineCount;
@Column(name = "type_count")
public int typeCount;
@Column(name = "test_file_count")
public int testFileCount;
@Column(name = "dependency_count")
public int dependencyCount;

    public static List<InventoryMetricsEntity> listByAssessment(UUID assessmentId) {
        return list("assessmentId = ?1", assessmentId);
    }

    public static long deleteByAssessment(UUID assessmentId) {
        return delete("assessmentId = ?1", assessmentId);
    }
}
