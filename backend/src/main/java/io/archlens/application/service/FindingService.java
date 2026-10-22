package io.archlens.application.service;

import io.archlens.api.dto.FindingDtos.*;
import io.archlens.domain.AuditAction;
import io.archlens.domain.FindingCertainty;
import io.archlens.domain.FindingOrigin;
import io.archlens.infrastructure.persistence.AssessmentEntity;
import io.archlens.infrastructure.persistence.FindingEntity;
import io.archlens.infrastructure.persistence.OrganizationEntity;
import io.archlens.infrastructure.security.IdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FindingService {

    @Inject
    OrganizationService organizationService;
    @Inject
    AuditService auditService;
    @Inject
    IdentityService identityService;

    public List<FindingResponse> list(UUID assessmentId) {
        OrganizationEntity org = ensureAssessment(assessmentId);
        return FindingEntity.listByOrgAndAssessment(org.id, assessmentId).stream().map(this::toResponse).toList();
    }

    public FindingResponse get(UUID assessmentId, UUID findingId) {
        OrganizationEntity org = ensureAssessment(assessmentId);
        FindingEntity f = FindingEntity.findByOrgAssessmentAndId(org.id, assessmentId, findingId);
        if (f == null) throw new NotFoundException("Achado não encontrado");
        return toResponse(f);
    }

    @Transactional
    public FindingResponse updateStatus(UUID assessmentId, UUID findingId, UpdateFindingStatusRequest req) {
        identityService.requireWrite();
        OrganizationEntity org = ensureAssessment(assessmentId);
        var actor = identityService.current().subject();

        FindingEntity f = FindingEntity.findByOrgAssessmentAndId(org.id, assessmentId, findingId);
        if (f == null) throw new NotFoundException("Achado não encontrado");

        f.status = req.status();
        f.reviewNote = req.reviewNote();
        f.reviewedBy = actor;
        f.reviewedAt = Instant.now();
        f.updatedAt = Instant.now();

        auditService.record(org.id, assessmentId, AuditAction.FINDING_STATUS_CHANGED, actor,
                "Achado " + f.ruleId + " -> " + req.status());

        return toResponse(f);
    }

    private OrganizationEntity ensureAssessment(UUID assessmentId) {
        OrganizationEntity org = organizationService.getOrganization();
        if (AssessmentEntity.findByOrgAndId(org.id, assessmentId) == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }
        return org;
    }

    private FindingResponse toResponse(FindingEntity f) {
        List<String> evidence = f.evidenceIds == null || f.evidenceIds.isBlank()
                ? List.of()
                : Arrays.asList(f.evidenceIds.split(","));
        return new FindingResponse(f.id, f.ruleId, f.title, f.description, f.severity, f.status,
                evidence, f.dimensionKey, f.reviewNote, f.reviewedBy, f.reviewedAt,
                f.origin == null ? FindingOrigin.AUTOMATED : f.origin,
                f.certainty == null ? FindingCertainty.HEURISTIC : f.certainty,
                f.createdAt, f.updatedAt);
    }
}
