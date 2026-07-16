package io.archlens.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.archlens.api.exception.BusinessException;
import io.archlens.application.model.AnalysisDocument;
import io.archlens.application.port.AnalysisSchemaValidator;
import io.archlens.domain.AssessmentStatus;
import io.archlens.domain.AuditAction;
import io.archlens.infrastructure.persistence.*;
import io.archlens.infrastructure.security.IdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AnalysisImportService {

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

    @Transactional
    public AnalysisImportEntity importAnalysis(UUID assessmentId, String idempotencyKey, String rawJson) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();

        AssessmentEntity assessment = AssessmentEntity.findByOrgAndId(org.id, assessmentId);
        if (assessment == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }

        AnalysisImportEntity existing = AnalysisImportEntity.findByIdempotency(org.id, assessmentId, idempotencyKey);
        if (existing != null) {
            return existing;
        }

        List<String> errors = schemaValidator.validate(rawJson);
        if (!errors.isEmpty()) {
            throw new BusinessException("JSON de análise inválido: " + String.join("; ", errors));
        }

        AnalysisDocument doc;
        try {
            doc = objectMapper.readValue(rawJson, AnalysisDocument.class);
        } catch (Exception e) {
            throw new BusinessException("Falha ao interpretar analysis.json: " + e.getMessage());
        }

        materializer.materialize(assessmentId, doc);

        FindingEntity.delete("assessmentId = ?1", assessmentId);
        List<FindingEntity> findings = ruleEngine.evaluate(assessmentId, org.id, doc);
        findings.forEach(f -> f.persist());

        dimensionScoreService.recalculateFromAnalysis(assessmentId, org.id, doc, findings);

        AnalysisImportEntity imp = new AnalysisImportEntity();
        imp.id = UUID.randomUUID();
        imp.organizationId = org.id;
        imp.assessmentId = assessmentId;
        imp.idempotencyKey = idempotencyKey;
        imp.contentHash = doc.contentHash();
        imp.schemaVersion = doc.schemaVersion();
        imp.analyzerVersion = doc.analyzerVersion();
        imp.rawJson = rawJson;
        imp.importedAt = Instant.now();
        imp.persist();

        assessment.status = AssessmentStatus.IMPORTED;
        assessment.projectRootName = doc.project().rootName();
        assessment.updatedAt = Instant.now();

        auditService.record(org.id, assessmentId, AuditAction.ANALYSIS_IMPORTED, actor,
                "Importação concluída com " + findings.size() + " achados");

        return imp;
    }
}
