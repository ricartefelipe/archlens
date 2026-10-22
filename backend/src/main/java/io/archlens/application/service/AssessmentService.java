package io.archlens.application.service;

import io.archlens.api.dto.AssessmentDtos.*;
import io.archlens.api.exception.BusinessException;
import io.archlens.domain.AssessmentStatus;
import io.archlens.domain.AuditAction;
import io.archlens.infrastructure.persistence.AnalysisImportEntity;
import io.archlens.infrastructure.persistence.AssessmentEntity;
import io.archlens.infrastructure.persistence.AuditEventEntity;
import io.archlens.infrastructure.persistence.BlobCleanupTaskEntity;
import io.archlens.infrastructure.persistence.DimensionScoreEntity;
import io.archlens.infrastructure.persistence.FindingEntity;
import io.archlens.infrastructure.persistence.ImportIdempotencyAliasEntity;
import io.archlens.infrastructure.persistence.ModernizationOptionEntity;
import io.archlens.infrastructure.persistence.QuestionnaireAnswerEntity;
import io.archlens.infrastructure.persistence.ReportSnapshotEntity;
import io.archlens.infrastructure.persistence.RoadmapItemEntity;
import io.archlens.infrastructure.security.IdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AssessmentService {

    private static final Map<AssessmentStatus, Set<AssessmentStatus>> TRANSITIONS = Map.of(
            AssessmentStatus.DRAFT, EnumSet.of(AssessmentStatus.IMPORTED, AssessmentStatus.IN_REVIEW, AssessmentStatus.COMPLETED),
            AssessmentStatus.IMPORTED, EnumSet.of(AssessmentStatus.IN_REVIEW, AssessmentStatus.COMPLETED, AssessmentStatus.DRAFT),
            AssessmentStatus.IN_REVIEW, EnumSet.of(AssessmentStatus.IMPORTED, AssessmentStatus.COMPLETED),
            AssessmentStatus.COMPLETED, EnumSet.noneOf(AssessmentStatus.class)
    );

    @Inject
    OrganizationService organizationService;
    @Inject
    AuditService auditService;
    @Inject
    IdentityService identityService;
    @Inject
    InventoryMaterializer inventoryMaterializer;

    @ConfigProperty(name = "archlens.retention.legal-hold", defaultValue = "false")
    boolean legalHold;

    @ConfigProperty(name = "archlens.retention.retain-audit", defaultValue = "true")
    boolean retainAudit;

    public List<AssessmentResponse> list() {
        var org = organizationService.getOrganization();
        return AssessmentEntity.listByOrg(org.id).stream().map(this::toResponse).toList();
    }

    public AssessmentResponse get(UUID id) {
        var org = organizationService.getOrganization();
        AssessmentEntity e = AssessmentEntity.findByOrgAndId(org.id, id);
        if (e == null) throw new NotFoundException("Avaliação não encontrada");
        return toResponse(e);
    }

    @Transactional
    public AssessmentResponse create(CreateAssessmentRequest req) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        Instant now = Instant.now();

        AssessmentEntity e = new AssessmentEntity();
        e.id = UUID.randomUUID();
        e.organizationId = org.id;
        e.name = req.name();
        e.description = req.description();
        e.status = AssessmentStatus.DRAFT;
        e.createdBy = actor;
        e.createdAt = now;
        e.updatedAt = now;
        e.persist();

        auditService.record(org.id, e.id, AuditAction.ASSESSMENT_CREATED, actor, "Avaliação criada: " + e.name);
        return toResponse(e);
    }

    @Transactional
    public AssessmentResponse update(UUID id, UpdateAssessmentRequest req) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        AssessmentEntity e = AssessmentEntity.findByOrgAndId(org.id, id);
        if (e == null) throw new NotFoundException("Avaliação não encontrada");

        if (req.name() != null) e.name = req.name();
        if (req.description() != null) e.description = req.description();
        if (req.status() != null && req.status() != e.status) {
            Set<AssessmentStatus> allowed = TRANSITIONS.getOrDefault(e.status, Set.of());
            if (!allowed.contains(req.status())) {
                throw new BusinessException("Transição de status inválida: " + e.status + " -> " + req.status());
            }
            e.status = req.status();
        }
        e.updatedAt = Instant.now();

        auditService.record(org.id, e.id, AuditAction.ASSESSMENT_UPDATED, actor, "Avaliação atualizada");
        return toResponse(e);
    }

    @Transactional
    public void delete(UUID id) {
        identityService.requireRole(io.archlens.domain.UserRole.ADMIN);
        DeletionPolicy.ensureDeletionAllowed(legalHold);
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        AssessmentEntity e = AssessmentEntity.findByOrgAndId(org.id, id);
        if (e == null) throw new NotFoundException("Avaliação não encontrada");

        Instant now = Instant.now();
        for (ReportSnapshotEntity snap : ReportSnapshotEntity.listByOrgAndAssessment(org.id, id)) {
            BlobCleanupTaskEntity task = new BlobCleanupTaskEntity();
            task.id = UUID.randomUUID();
            task.organizationId = org.id;
            task.assessmentId = id;
            task.snapshotId = snap.id;
            task.status = "PENDING";
            task.attempts = 0;
            task.createdAt = now;
            task.updatedAt = now;
            task.persist();
            snap.htmlContent = "";
            snap.pdfContent = null;
            snap.published = false;
            snap.assessmentId = null;
        }

        RoadmapItemEntity.delete("organizationId = ?1 and assessmentId = ?2", org.id, id);
        ModernizationOptionEntity.delete("organizationId = ?1 and assessmentId = ?2", org.id, id);
        DimensionScoreEntity.delete("organizationId = ?1 and assessmentId = ?2", org.id, id);
        QuestionnaireAnswerEntity.delete("organizationId = ?1 and assessmentId = ?2", org.id, id);
        FindingEntity.delete("organizationId = ?1 and assessmentId = ?2", org.id, id);
        ImportIdempotencyAliasEntity.delete("organizationId = ?1 and assessmentId = ?2", org.id, id);
        AnalysisImportEntity.delete("organizationId = ?1 and assessmentId = ?2", org.id, id);
        inventoryMaterializer.clear(org.id, id);

        if (!retainAudit) {
            AuditEventEntity.delete("organizationId = ?1 and assessmentId = ?2", org.id, id);
        }
        e.delete();
        auditService.record(org.id, id, AuditAction.ASSESSMENT_DELETED, actor,
                "Avaliação excluída; blobs de relatório ficam na fila de limpeza");
    }

    private AssessmentResponse toResponse(AssessmentEntity e) {
        return new AssessmentResponse(e.id, e.name, e.description, e.status, e.projectRootName, e.createdBy, e.createdAt, e.updatedAt);
    }
}
