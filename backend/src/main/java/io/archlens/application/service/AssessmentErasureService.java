package io.archlens.application.service;

import io.archlens.domain.AuditAction;
import io.archlens.domain.UserRole;
import io.archlens.infrastructure.persistence.AnalysisImportEntity;
import io.archlens.infrastructure.persistence.AssessmentEntity;
import io.archlens.infrastructure.persistence.AuditEventEntity;
import io.archlens.infrastructure.persistence.DimensionScoreEntity;
import io.archlens.infrastructure.persistence.FindingEntity;
import io.archlens.infrastructure.persistence.InventoryCycleEntity;
import io.archlens.infrastructure.persistence.InventoryDependencyEntity;
import io.archlens.infrastructure.persistence.InventoryEndpointEntity;
import io.archlens.infrastructure.persistence.InventoryHotspotEntity;
import io.archlens.infrastructure.persistence.InventoryMessagingEntity;
import io.archlens.infrastructure.persistence.InventoryMetricsEntity;
import io.archlens.infrastructure.persistence.InventoryModuleEntity;
import io.archlens.infrastructure.persistence.InventoryPackageEntity;
import io.archlens.infrastructure.persistence.InventoryPersistenceEntity;
import io.archlens.infrastructure.persistence.InventorySecretEntity;
import io.archlens.infrastructure.persistence.InventorySignalEntity;
import io.archlens.infrastructure.persistence.InventoryTypeEntity;
import io.archlens.infrastructure.persistence.ModernizationOptionEntity;
import io.archlens.infrastructure.persistence.QuestionnaireAnswerEntity;
import io.archlens.infrastructure.persistence.ReportSnapshotEntity;
import io.archlens.infrastructure.persistence.RoadmapItemEntity;
import io.archlens.infrastructure.security.IdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Retira os dados operacionais de uma avaliação (offboarding). A trilha de auditoria
 * permanece sem conteúdo de análise: detalhes anteriores são redigidos.
 */
@ApplicationScoped
public class AssessmentErasureService {

    private static final Logger LOG = Logger.getLogger(AssessmentErasureService.class);
    static final String REDACTED = "redigido na exclusão";
    private static final String SCOPE = "organizationId = ?1 and assessmentId = ?2";

    @Inject
    OrganizationService organizationService;
    @Inject
    IdentityService identityService;
    @Inject
    AuditService auditService;

    @Transactional
    public void erase(UUID assessmentId) {
        identityService.requireRole(UserRole.ADMIN);
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        AssessmentEntity assessment = AssessmentEntity.findByOrgAndId(org.id, assessmentId);
        if (assessment == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }

        AuditEventEntity.update(
                "details = ?1 where organizationId = ?2 and assessmentId = ?3",
                REDACTED, org.id, assessmentId);

        AnalysisImportEntity.delete(SCOPE, org.id, assessmentId);
        InventoryModuleEntity.delete(SCOPE, org.id, assessmentId);
        InventoryPackageEntity.delete(SCOPE, org.id, assessmentId);
        InventoryTypeEntity.delete(SCOPE, org.id, assessmentId);
        InventoryEndpointEntity.delete(SCOPE, org.id, assessmentId);
        InventoryPersistenceEntity.delete(SCOPE, org.id, assessmentId);
        InventoryMessagingEntity.delete(SCOPE, org.id, assessmentId);
        InventorySignalEntity.delete(SCOPE, org.id, assessmentId);
        InventoryDependencyEntity.delete(SCOPE, org.id, assessmentId);
        InventoryHotspotEntity.delete(SCOPE, org.id, assessmentId);
        InventoryCycleEntity.delete(SCOPE, org.id, assessmentId);
        InventorySecretEntity.delete(SCOPE, org.id, assessmentId);
        InventoryMetricsEntity.delete(SCOPE, org.id, assessmentId);
        FindingEntity.delete(SCOPE, org.id, assessmentId);
        QuestionnaireAnswerEntity.delete(SCOPE, org.id, assessmentId);
        DimensionScoreEntity.delete(SCOPE, org.id, assessmentId);
        RoadmapItemEntity.delete(SCOPE, org.id, assessmentId);
        ModernizationOptionEntity.delete(SCOPE, org.id, assessmentId);
        ReportSnapshotEntity.delete(SCOPE, org.id, assessmentId);

        assessment.delete();

        auditService.record(org.id, assessmentId, AuditAction.ASSESSMENT_ERASED, actor,
                "Dados da avaliação removidos do ambiente");
        LOG.infof("Exclusão de avaliação concluída assessmentId=%s actor=%s", assessmentId, actor);
    }
}
