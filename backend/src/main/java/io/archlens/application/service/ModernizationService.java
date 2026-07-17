package io.archlens.application.service;

import io.archlens.api.dto.CommonDtos.*;
import io.archlens.api.exception.BusinessException;
import io.archlens.domain.AuditAction;
import io.archlens.infrastructure.persistence.*;
import io.archlens.infrastructure.security.IdentityService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ModernizationService {

    private static final Set<String> EFFORTS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> IMPACTS = Set.of("LOW", "MEDIUM", "HIGH");
    private static final Set<String> PHASES = Set.of("SHORT_TERM", "MEDIUM_TERM", "LONG_TERM");

    @Inject
    OrganizationService organizationService;
    @Inject
    AuditService auditService;
    @Inject
    IdentityService identityService;

    public List<ModernizationOptionResponse> listOptions(UUID assessmentId) {
        UUID orgId = ensureAssessment(assessmentId);
        return ModernizationOptionEntity.listByOrgAndAssessment(orgId, assessmentId).stream()
                .map(this::toOption).toList();
    }

    @Transactional
    public ModernizationOptionResponse createOption(UUID assessmentId, ModernizationOptionRequest req) {
        identityService.requireWrite();
        UUID orgId = ensureAssessment(assessmentId);
        var actor = identityService.current().subject();
        validateOption(req);
        Instant now = Instant.now();

        ModernizationOptionEntity e = new ModernizationOptionEntity();
        e.id = UUID.randomUUID();
        e.organizationId = orgId;
        e.assessmentId = assessmentId;
        e.title = req.title().trim();
        e.description = req.description();
        e.effort = normalize(req.effort());
        e.impact = normalize(req.impact());
        e.createdAt = now;
        e.updatedAt = now;
        e.persist();

        auditService.record(orgId, assessmentId, AuditAction.MODERNIZATION_OPTION_CREATED, actor, e.title);
        return toOption(e);
    }

    @Transactional
    public ModernizationOptionResponse updateOption(UUID assessmentId, UUID optionId, ModernizationOptionRequest req) {
        identityService.requireWrite();
        UUID orgId = ensureAssessment(assessmentId);
        var actor = identityService.current().subject();
        validateOption(req);

        ModernizationOptionEntity e = ModernizationOptionEntity.findByOrgAssessmentAndId(orgId, assessmentId, optionId);
        if (e == null) throw new NotFoundException("Opção não encontrada");

        e.title = req.title().trim();
        e.description = req.description();
        e.effort = normalize(req.effort());
        e.impact = normalize(req.impact());
        e.updatedAt = Instant.now();

        auditService.record(orgId, assessmentId, AuditAction.MODERNIZATION_OPTION_UPDATED, actor, e.title);
        return toOption(e);
    }

    @Transactional
    public void deleteOption(UUID assessmentId, UUID optionId) {
        identityService.requireWrite();
        UUID orgId = ensureAssessment(assessmentId);
        ModernizationOptionEntity e = ModernizationOptionEntity.findByOrgAssessmentAndId(orgId, assessmentId, optionId);
        if (e == null) throw new NotFoundException("Opção não encontrada");
        // Snapshots publicados já materializaram HTML/PDF; exclusão não os altera.
        e.delete();
    }

    public List<RoadmapItemResponse> listRoadmap(UUID assessmentId) {
        UUID orgId = ensureAssessment(assessmentId);
        return RoadmapItemEntity.listByOrgAndAssessment(orgId, assessmentId).stream()
                .map(this::toRoadmap).toList();
    }

    @Transactional
    public RoadmapItemResponse createRoadmap(UUID assessmentId, RoadmapItemRequest req) {
        identityService.requireWrite();
        UUID orgId = ensureAssessment(assessmentId);
        var actor = identityService.current().subject();
        validateRoadmap(orgId, assessmentId, req);
        Instant now = Instant.now();

        RoadmapItemEntity e = new RoadmapItemEntity();
        e.id = UUID.randomUUID();
        e.organizationId = orgId;
        e.assessmentId = assessmentId;
        e.title = req.title().trim();
        e.description = req.description();
        e.phase = normalize(req.phase());
        e.sortOrder = req.sortOrder();
        e.modernizationOptionId = req.modernizationOptionId();
        e.createdAt = now;
        e.updatedAt = now;
        e.persist();

        auditService.record(orgId, assessmentId, AuditAction.ROADMAP_CREATED, actor, e.title);
        return toRoadmap(e);
    }

    @Transactional
    public RoadmapItemResponse updateRoadmap(UUID assessmentId, UUID itemId, RoadmapItemRequest req) {
        identityService.requireWrite();
        UUID orgId = ensureAssessment(assessmentId);
        var actor = identityService.current().subject();
        validateRoadmap(orgId, assessmentId, req);

        RoadmapItemEntity e = RoadmapItemEntity.findByOrgAssessmentAndId(orgId, assessmentId, itemId);
        if (e == null) throw new NotFoundException("Item não encontrado");

        e.title = req.title().trim();
        e.description = req.description();
        e.phase = normalize(req.phase());
        e.sortOrder = req.sortOrder();
        e.modernizationOptionId = req.modernizationOptionId();
        e.updatedAt = Instant.now();

        auditService.record(orgId, assessmentId, AuditAction.ROADMAP_UPDATED, actor, e.title);
        return toRoadmap(e);
    }

    @Transactional
    public void deleteRoadmap(UUID assessmentId, UUID itemId) {
        identityService.requireWrite();
        UUID orgId = ensureAssessment(assessmentId);
        RoadmapItemEntity e = RoadmapItemEntity.findByOrgAssessmentAndId(orgId, assessmentId, itemId);
        if (e == null) throw new NotFoundException("Item não encontrado");
        e.delete();
    }

    private UUID ensureAssessment(UUID assessmentId) {
        var org = organizationService.getOrganization();
        if (AssessmentEntity.findByOrgAndId(org.id, assessmentId) == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }
        return org.id;
    }

    private void validateOption(ModernizationOptionRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new BusinessException("Título da opção é obrigatório");
        }
        if (req.effort() == null || !EFFORTS.contains(normalize(req.effort()))) {
            throw new BusinessException("effort deve ser LOW, MEDIUM ou HIGH");
        }
        if (req.impact() == null || !IMPACTS.contains(normalize(req.impact()))) {
            throw new BusinessException("impact deve ser LOW, MEDIUM ou HIGH");
        }
    }

    private void validateRoadmap(UUID orgId, UUID assessmentId, RoadmapItemRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new BusinessException("Título do item de roadmap é obrigatório");
        }
        if (req.phase() == null || !PHASES.contains(normalize(req.phase()))) {
            throw new BusinessException("phase deve ser SHORT_TERM, MEDIUM_TERM ou LONG_TERM");
        }
        if (req.sortOrder() < 0) {
            throw new BusinessException("sortOrder deve ser >= 0");
        }
        if (req.modernizationOptionId() != null) {
            ModernizationOptionEntity opt = ModernizationOptionEntity.findByOrgAssessmentAndId(
                    orgId, assessmentId, req.modernizationOptionId());
            if (opt == null) {
                throw new BusinessException("modernizationOptionId inválido para esta avaliação/organização");
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }

    private ModernizationOptionResponse toOption(ModernizationOptionEntity e) {
        return new ModernizationOptionResponse(e.id, e.title, e.description, e.effort, e.impact, e.createdAt, e.updatedAt);
    }

    private RoadmapItemResponse toRoadmap(RoadmapItemEntity e) {
        return new RoadmapItemResponse(e.id, e.title, e.description, e.phase, e.sortOrder, e.modernizationOptionId, e.createdAt, e.updatedAt);
    }
}
