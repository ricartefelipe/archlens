package io.archlens.application.service;

import io.archlens.api.dto.AssessmentDtos.*;
import io.archlens.api.exception.BusinessException;
import io.archlens.domain.AssessmentStatus;
import io.archlens.domain.AuditAction;
import io.archlens.infrastructure.persistence.AssessmentEntity;
import io.archlens.infrastructure.security.IdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
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
        var org = organizationService.getOrganization();
        AssessmentEntity e = AssessmentEntity.findByOrgAndId(org.id, id);
        if (e == null) throw new NotFoundException("Avaliação não encontrada");
        e.delete();
    }

    private AssessmentResponse toResponse(AssessmentEntity e) {
        return new AssessmentResponse(e.id, e.name, e.description, e.status, e.projectRootName, e.createdBy, e.createdAt, e.updatedAt);
    }
}
