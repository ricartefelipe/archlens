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
import io.archlens.infrastructure.observability.ArchLensMetrics;
import io.archlens.infrastructure.persistence.*;
import io.archlens.infrastructure.security.IdentityService;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AnalysisImportService {

    private static final Logger LOG = Logger.getLogger(AnalysisImportService.class);

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
    @Inject
    ArchLensMetrics metrics;

    @ConfigProperty(name = "archlens.import.max-bytes", defaultValue = "5242880")
    long maxImportBytes;

    public record ImportResult(AnalysisImportEntity entity, int findingsCount) {}

    public ImportResult importAnalysis(UUID assessmentId, String idempotencyKey, String rawJson) {
        try {
            return QuarkusTransaction.requiringNew().call(() -> importInTransaction(assessmentId, idempotencyKey, rawJson));
        } catch (RuntimeException ex) {
            if (!isUniqueViolation(ex)) {
                throw ex;
            }
            RuntimeException last = ex;
            for (int attempt = 0; attempt < 3; attempt++) {
                try {
                    ImportResult replay = QuarkusTransaction.requiringNew()
                            .call(() -> replayAfterConstraint(assessmentId, idempotencyKey, rawJson));
                    if (replay != null) {
                        return replay;
                    }
                    break;
                } catch (RuntimeException replayEx) {
                    last = replayEx;
                    if (!isUniqueViolation(replayEx)) {
                        throw replayEx;
                    }
                }
            }
            throw last;
        }
    }

    ImportResult importInTransaction(UUID assessmentId, String idempotencyKey, String rawJson) {
        long started = System.currentTimeMillis();
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
            metrics.recordImportSchemaFailure();
            LOG.warnf("Import schema inválido assessmentId=%s errorCount=%d", assessmentId, errors.size());
            throw new BusinessException("JSON de análise inválido: " + String.join("; ", errors));
        }

        AnalysisDocument doc;
        try {
            doc = objectMapper.readValue(rawJson, AnalysisDocument.class);
        } catch (Exception e) {
            metrics.recordImportSchemaFailure();
            LOG.warnf("Falha ao interpretar analysis.json assessmentId=%s", assessmentId);
            throw new BusinessException("Falha ao interpretar analysis.json");
        }

        String computedHash = CanonicalContentHash.compute(rawJson);
        if (doc.contentHash() == null || !computedHash.equals(doc.contentHash())) {
            metrics.recordImportSchemaFailure();
            throw new BusinessException(
                    "contentHash divergente do conteúdo canônico (" + CanonicalContentHash.ALGORITHM + ")");
        }

        AnalysisImportEntity existing = findByKeyOrAlias(org.id, assessmentId, idempotencyKey);
        if (existing != null) {
            if (!existing.contentHash.equals(computedHash)) {
                throw new ConflictException(
                        "Idempotency-Key já usada com payload diferente (contentHash diverge)");
            }
            int count = FindingEntity.listByOrgAndAssessment(org.id, assessmentId).size();
            return new ImportResult(existing, count);
        }

        AnalysisImportEntity sameArtifact = AnalysisImportEntity.findByContentHash(org.id, assessmentId, computedHash);
        if (sameArtifact != null) {
            rememberKey(org.id, assessmentId, idempotencyKey, sameArtifact.id);
            int count = FindingEntity.listByOrgAndAssessment(org.id, assessmentId).size();
            return new ImportResult(sameArtifact, count);
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
        imp.contentHash = computedHash;
        imp.schemaVersion = doc.schemaVersion();
        imp.analyzerVersion = doc.analyzerVersion();
        // raw_json: apenas analysis.json (sem fonte). Política: tamanho limitado; retenção para auditoria de import.
        imp.rawJson = rawJson;
        imp.importedAt = Instant.now();
        imp.persist();
        AnalysisImportEntity.getEntityManager().flush();

        assessment.status = AssessmentStatus.IMPORTED;
        assessment.projectRootName = doc.project() != null ? doc.project().rootName() : null;
        assessment.updatedAt = Instant.now();

        auditService.record(org.id, assessmentId, AuditAction.ANALYSIS_IMPORTED, actor,
                "Importação concluída com " + persisted.size() + " achados; hash=" + computedHash
                        + "; algoritmo=" + CanonicalContentHash.ALGORITHM);

        long elapsed = System.currentTimeMillis() - started;
        metrics.recordImportSuccess(persisted.size(), elapsed);
        LOG.infof("Importação OK assessmentId=%s findings=%d durationMs=%d hash=%s",
                assessmentId, persisted.size(), elapsed, computedHash);

        return new ImportResult(imp, persisted.size());
    }

    private ImportResult replayAfterConstraint(UUID assessmentId, String idempotencyKey, String rawJson) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        String computedHash = CanonicalContentHash.compute(rawJson);
        AnalysisImportEntity byKey = findByKeyOrAlias(org.id, assessmentId, idempotencyKey);
        if (byKey != null) {
            if (!computedHash.equals(byKey.contentHash)) {
                throw new ConflictException(
                        "Idempotency-Key já usada com payload diferente (contentHash diverge)");
            }
            int count = FindingEntity.listByOrgAndAssessment(org.id, assessmentId).size();
            return new ImportResult(byKey, count);
        }
        AnalysisImportEntity byHash = AnalysisImportEntity.findByContentHash(org.id, assessmentId, computedHash);
        if (byHash == null) {
            return null;
        }
        rememberKey(org.id, assessmentId, idempotencyKey, byHash.id);
        int count = FindingEntity.listByOrgAndAssessment(org.id, assessmentId).size();
        return new ImportResult(byHash, count);
    }

    private AnalysisImportEntity findByKeyOrAlias(UUID orgId, UUID assessmentId, String idempotencyKey) {
        AnalysisImportEntity direct = AnalysisImportEntity.findByIdempotency(orgId, assessmentId, idempotencyKey);
        if (direct != null) {
            return direct;
        }
        ImportIdempotencyAliasEntity alias = ImportIdempotencyAliasEntity.findKey(orgId, assessmentId, idempotencyKey);
        if (alias == null) {
            return null;
        }
        return AnalysisImportEntity.findById(alias.importId);
    }

    private void rememberKey(UUID orgId, UUID assessmentId, String idempotencyKey, UUID importId) {
        if (ImportIdempotencyAliasEntity.findKey(orgId, assessmentId, idempotencyKey) != null) {
            return;
        }
        ImportIdempotencyAliasEntity alias = new ImportIdempotencyAliasEntity();
        alias.id = UUID.randomUUID();
        alias.organizationId = orgId;
        alias.assessmentId = assessmentId;
        alias.idempotencyKey = idempotencyKey;
        alias.importId = importId;
        alias.persist();
        ImportIdempotencyAliasEntity.getEntityManager().flush();
    }

    static boolean isUniqueViolation(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String type = current.getClass().getName();
            if (type.contains("ConstraintViolation")) {
                return true;
            }
            String message = current.getMessage();
            if (message != null && (message.contains("uk_import_org_assessment_hash")
                    || message.contains("uk_import_idempotency")
                    || message.contains("uk_import_alias"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
