package io.archlens.report;

import io.archlens.application.port.NarrativeGenerator;
import io.archlens.domain.AuditAction;
import io.archlens.infrastructure.persistence.*;
import io.archlens.infrastructure.security.IdentityService;
import io.archlens.application.service.AuditService;
import io.archlens.application.service.OrganizationService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class ReportService {

    @Inject
    OrganizationService organizationService;
    @Inject
    NarrativeGenerator narrativeGenerator;
    @Inject
    AuditService auditService;
    @Inject
    IdentityService identityService;

    @Transactional
    public ReportSnapshotEntity generate(UUID assessmentId) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        AssessmentEntity assessment = AssessmentEntity.findByOrgAndId(org.id, assessmentId);
        if (assessment == null) throw new NotFoundException("Avaliação não encontrada");

        String html = buildHtml(assessment);
        byte[] pdf = renderPdf(html);
        String hash = sha256(html);

        int nextVersion = ReportSnapshotEntity.listByAssessment(assessmentId).stream()
                .mapToInt(r -> r.version)
                .max()
                .orElse(0) + 1;

        ReportSnapshotEntity snap = new ReportSnapshotEntity();
        snap.id = UUID.randomUUID();
        snap.organizationId = org.id;
        snap.assessmentId = assessmentId;
        snap.version = nextVersion;
        snap.contentHash = "sha256:" + hash;
        snap.htmlContent = html;
        snap.pdfContent = pdf;
        snap.published = false;
        snap.createdAt = Instant.now();
        snap.persist();

        auditService.record(org.id, assessmentId, AuditAction.REPORT_GENERATED, actor, "Relatório v" + nextVersion);
        return snap;
    }

    @Transactional
    public ReportSnapshotEntity publish(UUID assessmentId, UUID snapshotId) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();

        ReportSnapshotEntity snap = ReportSnapshotEntity.findById(snapshotId);
        if (snap == null || !snap.assessmentId.equals(assessmentId)) {
            throw new NotFoundException("Snapshot não encontrado");
        }
        if (snap.published) {
            throw new jakarta.ws.rs.BadRequestException("Snapshot já publicado e imutável");
        }

        snap.published = true;
        snap.publishedAt = Instant.now();
        snap.publishedBy = actor;

        auditService.record(org.id, assessmentId, AuditAction.REPORT_PUBLISHED, actor,
                "Snapshot v" + snap.version + " publicado");
        return snap;
    }

    public ReportSnapshotEntity getSnapshot(UUID assessmentId, UUID snapshotId) {
        ReportSnapshotEntity snap = ReportSnapshotEntity.findById(snapshotId);
        if (snap == null || !snap.assessmentId.equals(assessmentId)) {
            throw new NotFoundException("Snapshot não encontrado");
        }
        return snap;
    }

    public List<ReportSnapshotEntity> list(UUID assessmentId) {
        return ReportSnapshotEntity.listByAssessment(assessmentId);
    }

    private String buildHtml(AssessmentEntity assessment) {
        var findings = FindingEntity.listByAssessment(assessment.id);
        var dimensions = DimensionScoreEntity.listByAssessment(assessment.id);
        String summary = narrativeGenerator.generateExecutiveSummary(assessment.id, Map.of());

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\"/>");
        sb.append("<title>ArchLens — ").append(escape(assessment.name)).append("</title>");
        sb.append("<style>body{font-family:Segoe UI,Arial,sans-serif;margin:40px;color:#1a1a2e}");
        sb.append("h1{color:#16213e}table{border-collapse:collapse;width:100%;margin:16px 0}");
        sb.append("th,td{border:1px solid #ccc;padding:8px;text-align:left}th{background:#e8eef5}</style></head><body>");
        sb.append("<h1>Relatório ArchLens</h1>");
        sb.append("<p><strong>Avaliação:</strong> ").append(escape(assessment.name)).append("</p>");
        sb.append("<p><strong>Gerado em:</strong> ")
                .append(DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now()))
                .append(" UTC</p>");
        sb.append("<h2>Resumo executivo</h2><p>").append(escape(summary)).append("</p>");
        sb.append("<h2>Dimensões</h2><table><tr><th>Dimensão</th><th>Score</th></tr>");
        for (var d : dimensions) {
            sb.append("<tr><td>").append(d.dimensionKey).append("</td><td>").append(d.score).append("</td></tr>");
        }
        sb.append("</table><h2>Achados</h2><table><tr><th>Regra</th><th>Severidade</th><th>Status</th></tr>");
        for (var f : findings) {
            sb.append("<tr><td>").append(escape(f.title)).append("</td><td>")
                    .append(f.severity).append("</td><td>").append(f.status).append("</td></tr>");
        }
        sb.append("</table></body></html>");
        return sb.toString();
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private String sha256(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
