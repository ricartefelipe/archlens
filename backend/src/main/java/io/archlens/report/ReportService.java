package io.archlens.report;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import io.archlens.api.exception.ConflictException;
import io.archlens.application.port.NarrativeGenerator;
import io.archlens.application.service.AuditService;
import io.archlens.application.service.OrganizationService;
import io.archlens.domain.AuditAction;
import io.archlens.infrastructure.observability.ArchLensMetrics;
import io.archlens.infrastructure.persistence.*;
import io.archlens.infrastructure.security.IdentityService;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class ReportService {

    private static final Logger LOG = Logger.getLogger(ReportService.class);
    private static final int MIN_PDF_BYTES = 100;

    @Inject
    OrganizationService organizationService;
    @Inject
    NarrativeGenerator narrativeGenerator;
    @Inject
    AuditService auditService;
    @Inject
    IdentityService identityService;
    @Inject
    ArchLensMetrics metrics;

    @ConfigProperty(name = "archlens.scores.disclaimer", defaultValue = "Scores apoiam decisão; não são precisão científica.")
    String scoresDisclaimer;

    @Transactional
    public ReportSnapshotEntity generate(UUID assessmentId) {
        long started = System.currentTimeMillis();
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        AssessmentEntity assessment = AssessmentEntity.findByOrgAndId(org.id, assessmentId);
        if (assessment == null) throw new NotFoundException("Avaliação não encontrada");

        String html = buildHtml(org.id, assessment);
        byte[] pdf;
        try {
            pdf = renderPdf(html);
        } catch (RuntimeException e) {
            metrics.recordReportPdfFailure();
            LOG.errorf(e, "Falha ao gerar PDF assessmentId=%s", assessmentId);
            throw e;
        }
        String hash = sha256(html);

        int nextVersion = ReportSnapshotEntity.listByOrgAndAssessment(org.id, assessmentId).stream()
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
        metrics.recordReportSuccess(System.currentTimeMillis() - started);
        LOG.infof("Relatório gerado assessmentId=%s version=%d durationMs=%d",
                assessmentId, nextVersion, System.currentTimeMillis() - started);
        return snap;
    }

    @Transactional
    public ReportSnapshotEntity publish(UUID assessmentId, UUID snapshotId) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();

        if (AssessmentEntity.findByOrgAndId(org.id, assessmentId) == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }

        ReportSnapshotEntity snap = ReportSnapshotEntity.findByOrgAssessmentAndId(org.id, assessmentId, snapshotId);
        if (snap == null) {
            throw new NotFoundException("Snapshot não encontrado");
        }
        if (snap.published) {
            throw new ConflictException("Snapshot já publicado e imutável");
        }

        snap.published = true;
        snap.publishedAt = Instant.now();
        snap.publishedBy = actor;

        auditService.record(org.id, assessmentId, AuditAction.REPORT_PUBLISHED, actor,
                "Snapshot v" + snap.version + " publicado");
        return snap;
    }

    public ReportSnapshotEntity getSnapshot(UUID assessmentId, UUID snapshotId) {
        var org = organizationService.getOrganization();
        if (AssessmentEntity.findByOrgAndId(org.id, assessmentId) == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }
        ReportSnapshotEntity snap = ReportSnapshotEntity.findByOrgAssessmentAndId(org.id, assessmentId, snapshotId);
        if (snap == null) {
            throw new NotFoundException("Snapshot não encontrado");
        }
        return snap;
    }

    public List<ReportSnapshotEntity> list(UUID assessmentId) {
        var org = organizationService.getOrganization();
        if (AssessmentEntity.findByOrgAndId(org.id, assessmentId) == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }
        return ReportSnapshotEntity.listByOrgAndAssessment(org.id, assessmentId);
    }

    private String buildHtml(UUID orgId, AssessmentEntity assessment) {
        var findings = FindingEntity.listByOrgAndAssessment(orgId, assessment.id);
        var dimensions = DimensionScoreEntity.listByOrgAndAssessment(orgId, assessment.id);
        var options = ModernizationOptionEntity.listByOrgAndAssessment(orgId, assessment.id);
        var roadmap = RoadmapItemEntity.listByOrgAndAssessment(orgId, assessment.id);
        List<InventoryModuleEntity> modules = InventoryModuleEntity.list(
                "organizationId = ?1 and assessmentId = ?2", orgId, assessment.id);
        InventoryMetricsEntity metrics = InventoryMetricsEntity.find(
                "organizationId = ?1 and assessmentId = ?2", orgId, assessment.id)
                .firstResult();
        AnalysisImportEntity lastImport = AnalysisImportEntity
                .find("organizationId = ?1 and assessmentId = ?2 order by importedAt desc", orgId, assessment.id)
                .firstResult();

        String summary = narrativeGenerator.generateExecutiveSummary(assessment.id, Map.of(
                "organizationId", orgId.toString()
        ));

        String generatedAt = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(Instant.now());
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html><html lang=\"pt-BR\"><head><meta charset=\"UTF-8\"/>");
        sb.append("<title>ArchLens — ").append(escape(assessment.name)).append("</title>");
        sb.append("<style>body{font-family:Segoe UI,Arial,sans-serif;margin:40px;color:#1a1a2e}");
        sb.append("h1,h2{color:#16213e}table{border-collapse:collapse;width:100%;margin:16px 0}");
        sb.append("th,td{border:1px solid #ccc;padding:8px;text-align:left}th{background:#e8eef5}");
        sb.append("td{word-wrap:break-word}");
        sb.append(".meta{color:#555;font-size:0.95em}.note{background:#f7f7f7;padding:12px;border-left:4px solid #888}");
        sb.append("@media print{body{margin:12mm}a{color:#000;text-decoration:none}h2{page-break-after:avoid}}</style></head><body>");
        sb.append("<h1>Relatório ArchLens</h1>");
        sb.append("<p class=\"meta\"><strong>Avaliação:</strong> ").append(escape(assessment.name)).append("</p>");
        sb.append("<p class=\"meta\"><strong>Escopo:</strong> ")
                .append(escape(assessment.projectRootName != null ? assessment.projectRootName : "(não importado)"))
                .append("</p>");
        sb.append("<p class=\"meta\"><strong>Gerado em:</strong> ").append(generatedAt).append(" UTC</p>");
        if (lastImport != null) {
            sb.append("<p class=\"meta\"><strong>Analyzer:</strong> ")
                    .append(escape(lastImport.analyzerVersion))
                    .append(" · schema ").append(escape(lastImport.schemaVersion))
                    .append(" · hash ").append(escape(lastImport.contentHash)).append("</p>");
        }

        sb.append("<h2>Resumo executivo</h2><p>").append(escape(summary)).append("</p>");
        sb.append("<p class=\"note\">O resumo é apoio à leitura. Achados abaixo são candidatos heurísticos, não falhas comprovadas.</p>");

        sb.append("<h2>Diagnóstico técnico</h2>");
        sb.append("<table><tr><th>Título</th><th>Severidade</th><th>Impacto na dimensão</th><th>Status</th><th>Evidências</th></tr>");
        for (var f : findings) {
            sb.append("<tr><td>").append(escape(f.title)).append("</td><td>")
                    .append(escape(f.severity.name())).append("</td><td>")
                    .append(f.dimensionKey == null ? "" : escape(f.dimensionKey.name())).append("</td><td>")
                    .append(escape(f.status.name())).append("</td><td>")
                    .append(escape(f.evidenceIds)).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<h2>Trilha de evidências</h2><ul>");
        for (var f : findings) {
            sb.append("<li>").append(escape(f.ruleId)).append(": ")
                    .append(escape(f.evidenceIds)).append("</li>");
        }
        sb.append("</ul>");
        sb.append("<h2>Incerteza</h2><p class=\"note\">Sinais e regras são heurísticos. A severidade indica prioridade de revisão, não exploração demonstrada. Esforço e impacto das opções de modernização são estimativas humanas.</p>");

        sb.append("<h2>Inventário (resumo)</h2><table><tr><th>Módulo</th><th>Build</th><th>Path</th></tr>");
        for (InventoryModuleEntity m : modules) {
            sb.append("<tr><td>").append(escape(m.name)).append("</td><td>")
                    .append(escape(m.buildSystem)).append("</td><td>")
                    .append(escape(m.path)).append("</td></tr>");
        }
        sb.append("</table>");
        if (metrics != null) {
            sb.append("<p>Arquivos: ").append(metrics.fileCount)
                    .append(" · Java: ").append(metrics.javaFileCount)
                    .append(" · Linhas: ").append(metrics.lineCount)
                    .append(" · Tipos: ").append(metrics.typeCount)
                    .append(" · Deps: ").append(metrics.dependencyCount).append("</p>");
        }

        sb.append("<h2>Scores por dimensão</h2>");
        sb.append("<p class=\"note\">").append(escape(scoresDisclaimer)).append("</p>");
        sb.append("<table><tr><th>Dimensão</th><th>Score</th><th>Rationale</th></tr>");
        for (var d : dimensions) {
            sb.append("<tr><td>").append(escape(d.dimensionKey.name())).append("</td><td>")
                    .append(d.score).append("</td><td>")
                    .append(escape(d.rationale)).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<h2>Achados candidatos à revisão</h2><table><tr><th>Regra</th><th>Severidade</th><th>Status</th><th>Nota</th></tr>");
        for (var f : findings) {
            sb.append("<tr><td>").append(escape(f.title)).append("</td><td>")
                    .append(escape(f.severity.name())).append("</td><td>")
                    .append(escape(f.status.name())).append("</td><td>")
                    .append(escape(f.reviewNote)).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<h2>Opções de modernização</h2><table><tr><th>Título</th><th>Esforço</th><th>Impacto</th></tr>");
        for (var o : options) {
            sb.append("<tr><td>").append(escape(o.title)).append("</td><td>")
                    .append(escape(o.effort)).append("</td><td>")
                    .append(escape(o.impact)).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<h2>Roadmap</h2><table><tr><th>Ordem</th><th>Fase</th><th>Título</th></tr>");
        for (var r : roadmap) {
            sb.append("<tr><td>").append(r.sortOrder).append("</td><td>")
                    .append(escape(r.phase)).append("</td><td>")
                    .append(escape(r.title)).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<h2>Limitações</h2>");
        sb.append("<ul>");
        sb.append("<li>Análise estática offline; não executa o build do alvo.</li>");
        sb.append("<li>Código-fonte não é enviado a serviços externos.</li>");
        sb.append("<li>Scores e recomendações apoiam decisão humana; não constituem precisão científica.</li>");
        sb.append("<li>Narrativa LLM desabilitada por padrão; fallback determinístico usado neste relatório.</li>");
        sb.append("</ul>");

        sb.append("<h2>Metadados</h2>");
        sb.append("<p>organizationId=").append(orgId)
                .append(" · assessmentId=").append(assessment.id)
                .append(" · status=").append(escape(assessment.status.name())).append("</p>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            byte[] pdf = out.toByteArray();
            if (pdf.length < MIN_PDF_BYTES || !(pdf[0] == '%' && pdf[1] == 'P' && pdf[2] == 'D' && pdf[3] == 'F')) {
                throw new IllegalStateException("PDF gerado inválido (tamanho=" + pdf.length + ")");
            }
            return pdf;
        } catch (Exception e) {
            LOG.error("Falha ao renderizar PDF", e);
            throw new IllegalStateException("Falha ao gerar PDF do relatório", e);
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
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
