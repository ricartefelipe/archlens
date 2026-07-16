package io.archlens.application.service;

import io.archlens.api.dto.CommonDtos.*;
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
public class ModernizationService {

    @Inject
    OrganizationService organizationService;
    @Inject
    AuditService auditService;
    @Inject
    IdentityService identityService;

    public List<ModernizationOptionResponse> listOptions(UUID assessmentId) {
        ensureAssessment(assessmentId);
        return ModernizationOptionEntity.listByAssessment(assessmentId).stream().map(this::toOption).toList();
    }

    @Transactional
    public ModernizationOptionResponse createOption(UUID assessmentId, ModernizationOptionRequest req) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        ensureAssessment(assessmentId);
        Instant now = Instant.now();

        ModernizationOptionEntity e = new ModernizationOptionEntity();
        e.id = UUID.randomUUID();
        e.organizationId = org.id;
        e.assessmentId = assessmentId;
        e.title = req.title();
        e.description = req.description();
        e.effort = req.effort();
        e.impact = req.impact();
        e.createdAt = now;
        e.updatedAt = now;
        e.persist();

        auditService.record(org.id, assessmentId, AuditAction.MODERNIZATION_OPTION_CREATED, actor, e.title);
        return toOption(e);
    }

    @Transactional
    public ModernizationOptionResponse updateOption(UUID assessmentId, UUID optionId, ModernizationOptionRequest req) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        ensureAssessment(assessmentId);

        ModernizationOptionEntity e = ModernizationOptionEntity.findById(optionId);
        if (e == null || !e.assessmentId.equals(assessmentId)) throw new NotFoundException("Opção não encontrada");

        e.title = req.title();
        e.description = req.description();
        e.effort = req.effort();
        e.impact = req.impact();
        e.updatedAt = Instant.now();

        auditService.record(org.id, assessmentId, AuditAction.MODERNIZATION_OPTION_UPDATED, actor, e.title);
        return toOption(e);
    }

    @Transactional
    public void deleteOption(UUID assessmentId, UUID optionId) {
        identityService.requireWrite();
        ensureAssessment(assessmentId);
        ModernizationOptionEntity e = ModernizationOptionEntity.findById(optionId);
        if (e == null || !e.assessmentId.equals(assessmentId)) throw new NotFoundException("Opção não encontrada");
        e.delete();
    }

    public List<RoadmapItemResponse> listRoadmap(UUID assessmentId) {
        ensureAssessment(assessmentId);
        return RoadmapItemEntity.listByAssessment(assessmentId).stream().map(this::toRoadmap).toList();
    }

    @Transactional
    public RoadmapItemResponse createRoadmap(UUID assessmentId, RoadmapItemRequest req) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        ensureAssessment(assessmentId);
        Instant now = Instant.now();

        RoadmapItemEntity e = new RoadmapItemEntity();
        e.id = UUID.randomUUID();
        e.organizationId = org.id;
        e.assessmentId = assessmentId;
        e.title = req.title();
        e.description = req.description();
        e.phase = req.phase();
        e.sortOrder = req.sortOrder();
        e.modernizationOptionId = req.modernizationOptionId();
        e.createdAt = now;
        e.updatedAt = now;
        e.persist();

        auditService.record(org.id, assessmentId, AuditAction.ROADMAP_CREATED, actor, e.title);
        return toRoadmap(e);
    }

    @Transactional
    public RoadmapItemResponse updateRoadmap(UUID assessmentId, UUID itemId, RoadmapItemRequest req) {
        identityService.requireWrite();
        var org = organizationService.getOrganization();
        var actor = identityService.current().subject();
        ensureAssessment(assessmentId);

        RoadmapItemEntity e = RoadmapItemEntity.findById(itemId);
        if (e == null || !e.assessmentId.equals(assessmentId)) throw new NotFoundException("Item não encontrado");

        e.title = req.title();
        e.description = req.description();
        e.phase = req.phase();
        e.sortOrder = req.sortOrder();
        e.modernizationOptionId = req.modernizationOptionId();
        e.updatedAt = Instant.now();

        auditService.record(org.id, assessmentId, AuditAction.ROADMAP_UPDATED, actor, e.title);
        return toRoadmap(e);
    }

    @Transactional
    public void deleteRoadmap(UUID assessmentId, UUID itemId) {
        identityService.requireWrite();
        ensureAssessment(assessmentId);
        RoadmapItemEntity e = RoadmapItemEntity.findById(itemId);
        if (e == null || !e.assessmentId.equals(assessmentId)) throw new NotFoundException("Item não encontrado");
        e.delete();
    }

    private void ensureAssessment(UUID assessmentId) {
        var org = organizationService.getOrganization();
        if (AssessmentEntity.findByOrgAndId(org.id, assessmentId) == null) {
            throw new NotFoundException("Avaliação não encontrada");
        }
    }

    private ModernizationOptionResponse toOption(ModernizationOptionEntity e) {
        return new ModernizationOptionResponse(e.id, e.title, e.description, e.effort, e.impact, e.createdAt, e.updatedAt);
    }

    private RoadmapItemResponse toRoadmap(RoadmapItemEntity e) {
        return new RoadmapItemResponse(e.id, e.title, e.description, e.phase, e.sortOrder, e.modernizationOptionId, e.createdAt, e.updatedAt);
    }
}
