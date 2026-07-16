package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_import")
public class AnalysisImportEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    @Column(name = "idempotency_key")
    public String idempotencyKey;
    @Column(name = "content_hash")
    public String contentHash;
    @Column(name = "schema_version")
    public String schemaVersion;
    @Column(name = "analyzer_version")
    public String analyzerVersion;
    @Lob
    @Column(name = "raw_json")
    public String rawJson;
    @Column(name = "imported_at")
    public Instant importedAt;

    public static AnalysisImportEntity findByIdempotency(UUID orgId, UUID assessmentId, String key) {
        return find("organizationId = ?1 and assessmentId = ?2 and idempotencyKey = ?3",
                orgId, assessmentId, key).firstResult();
    }
}
