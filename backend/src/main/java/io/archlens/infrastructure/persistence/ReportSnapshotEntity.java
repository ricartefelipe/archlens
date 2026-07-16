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
    @Lob
    @Column(name = "html_content")
    public String htmlContent;
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "pdf_content", columnDefinition = "VARBINARY(10000000)")
    public byte[] pdfContent;
    public boolean published;
    @Column(name = "published_at")
    public Instant publishedAt;
    @Column(name = "published_by")
    public String publishedBy;
    @Column(name = "created_at")
    public Instant createdAt;

    public static List<ReportSnapshotEntity> listByAssessment(UUID assessmentId) {
        return list("assessmentId = ?1 order by version desc", assessmentId);
    }

    public static ReportSnapshotEntity findPublished(UUID assessmentId) {
        return find("assessmentId = ?1 and published = true order by version desc", assessmentId).firstResult();
    }
}
