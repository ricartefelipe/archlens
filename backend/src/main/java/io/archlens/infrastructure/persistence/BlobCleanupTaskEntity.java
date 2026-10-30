package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "blob_cleanup_task")
public class BlobCleanupTaskEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    @Column(name = "snapshot_id", columnDefinition = "uuid")
    public UUID snapshotId;
    public String status;
    public int attempts;
    @Column(name = "last_error", columnDefinition = "text")
    public String lastError;
    @Column(name = "created_at")
    public Instant createdAt;
    @Column(name = "updated_at")
    public Instant updatedAt;

    public static List<BlobCleanupTaskEntity> listPending() {
        return list("status = ?1 order by createdAt", "PENDING");
    }
}
