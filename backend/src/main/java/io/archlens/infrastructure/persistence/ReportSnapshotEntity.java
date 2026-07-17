package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "report_snapshot")
public class ReportSnapshotEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    public int version;
    @Column(name = "content_hash")
    public String contentHash;
    @Column(name = "html_content", columnDefinition = "text")
    public String htmlContent;
    @JdbcTypeCode(SqlTypes.BINARY)
    @Column(name = "pdf_content", columnDefinition = "bytea")
    public byte[] pdfContent;
    public boolean published;
    @Column(name = "published_at")
    public Instant publishedAt;
    @Column(name = "published_by")
    public String publishedBy;
    @Column(name = "created_at")
    public Instant createdAt;

    public static List<ReportSnapshotEntity> listByOrgAndAssessment(UUID orgId, UUID assessmentId) {
        return list("organizationId = ?1 and assessmentId = ?2 order by version desc", orgId, assessmentId);
    }

    public static ReportSnapshotEntity findByOrgAssessmentAndId(UUID orgId, UUID assessmentId, UUID id) {
        return find("organizationId = ?1 and assessmentId = ?2 and id = ?3", orgId, assessmentId, id)
                .firstResult();
    }

    public static ReportSnapshotEntity findPublished(UUID orgId, UUID assessmentId) {
        return find("organizationId = ?1 and assessmentId = ?2 and published = true order by version desc",
                orgId, assessmentId).firstResult();
    }
}
