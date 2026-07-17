package io.archlens.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.archlens.api.exception.BusinessException;
import io.archlens.api.exception.ConflictException;
import io.archlens.api.exception.PayloadTooLargeException;
import io.archlens.application.model.AnalysisDocument;
import io.archlens.application.port.AnalysisSchemaValidator;
import io.archlens.domain.AssessmentStatus;
import io.archlens.domain.AuditAction;
import io.archlens.domain.FindingStatus;
import io.archlens.infrastructure.persistence.*;
import io.archlens.infrastructure.security.IdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class AnalysisImportService {

    private static final Set<AssessmentStatus> IMPORTABLE = EnumSet.of(
            AssessmentStatus.DRAFT,
            AssessmentStatus.IMPORTED,
            AssessmentStatus.IN_REVIEW
    );

    @Inject
    AnalysisSchemaValidator schemaValidator;
    @Inject
    ObjectMapper objectMapper;
    @Inject
    InventoryMaterializer materializer;
    @Inject
    RuleEngine ruleEngine;
    @Inject
    AuditService auditService;
    @Inject
    DimensionScoreService dimensionScoreService;
    @Inject
    IdentityService identityService;
    @Inject
    OrganizationService organizationService;

    @ConfigProperty(name = "archlens.import.max-bytes", defaultValue = "5242880")
    long maxImportBytes;

    public record ImportResult(AnalysisImportEntity entity, int findingsCount) {}

    @Transactional
    public ImportResult importAnalysis(UUID assessmentId, String idempotencyKey, String rawJson) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();

        if (rawJson == null) {
            throw new BusinessException("Corpo da importação é obrigatório");
        }
        long size = rawJson.getBytes(StandardCharsets.UTF_8).length;
        if (size > maxImportBytes) {
            throw new PayloadTooLargeException(
                    "Payload excede o limite de " + maxImportBytes + " bytes");
        }

        AssessmentEntity assessment = AssessmentEntity.findByOrgAndId(org.id, assessmentId);
        if (assessment == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }
        if (!IMPORTABLE.contains(assessment.status)) {
            throw new BusinessException("Importação não permitida no status " + assessment.status);
        }

        List<String> errors = schemaValidator.validate(rawJson);
        if (!errors.isEmpty()) {
            throw new BusinessException("JSON de análise inválido: " + String.join("; ", errors));
        }

        AnalysisDocument doc;
        try {
            doc = objectMapper.readValue(rawJson, AnalysisDocument.class);
        } catch (Exception e) {
            throw new BusinessException("Falha ao interpretar analysis.json");
        }

        AnalysisImportEntity existing = AnalysisImportEntity.findByIdempotency(org.id, assessmentId, idempotencyKey);
        if (existing != null) {
            if (!existing.contentHash.equals(doc.contentHash())) {
                throw new ConflictException(
                        "Idempotency-Key já usada com payload diferente (contentHash diverge)");
            }
            int count = FindingEntity.listByOrgAndAssessment(org.id, assessmentId).size();
            return new ImportResult(existing, count);
        }

        materializer.materialize(org.id, assessmentId, doc);

        List<FindingEntity> evaluated = ruleEngine.evaluate(assessmentId, org.id, doc);
        mergeFindings(org.id, assessmentId, evaluated);

        List<FindingEntity> persisted = FindingEntity.listByOrgAndAssessment(org.id, assessmentId);
        dimensionScoreService.recalculateFromAnalysis(assessmentId, org.id, doc, persisted);

        AnalysisImportEntity imp = new AnalysisImportEntity();
        imp.id = UUID.randomUUID();
        imp.organizationId = org.id;
        imp.assessmentId = assessmentId;
        imp.idempotencyKey = idempotencyKey;
        imp.contentHash = doc.contentHash();
        imp.schemaVersion = doc.schemaVersion();
        imp.analyzerVersion = doc.analyzerVersion();
        // raw_json: apenas analysis.json (sem fonte). Política: tamanho limitado; retenção para auditoria de import.
        imp.rawJson = rawJson;
        imp.importedAt = Instant.now();
        imp.persist();

        assessment.status = AssessmentStatus.IMPORTED;
        assessment.projectRootName = doc.project() != null ? doc.project().rootName() : null;
        assessment.updatedAt = Instant.now();

        auditService.record(org.id, assessmentId, AuditAction.ANALYSIS_IMPORTED, actor,
                "Importação concluída com " + persisted.size() + " achados; hash=" + doc.contentHash());

        return new ImportResult(imp, persisted.size());
    }

    private void mergeFindings(UUID orgId, UUID assessmentId, List<FindingEntity> evaluated) {
        Set<String> matched = new HashSet<>();
        Instant now = Instant.now();

        for (FindingEntity neu : evaluated) {
            matched.add(neu.ruleId);
            FindingEntity old = FindingEntity.findByOrgAssessmentAndRule(orgId, assessmentId, neu.ruleId);
            if (old == null) {
                neu.persist();
                continue;
            }
            old.title = neu.title;
            old.description = neu.description;
            old.severity = neu.severity;
            old.evidenceIds = neu.evidenceIds;
            old.dimensionKey = neu.dimensionKey;
            old.updatedAt = now;
            // preserva status, reviewNote, reviewedBy, reviewedAt
        }

        for (FindingEntity old : FindingEntity.listByOrgAndAssessment(orgId, assessmentId)) {
            if (matched.contains(old.ruleId)) {
                continue;
            }
            if (old.status == FindingStatus.OPEN || old.status == FindingStatus.DISMISSED) {
                old.delete();
            }
        }
    }
}
