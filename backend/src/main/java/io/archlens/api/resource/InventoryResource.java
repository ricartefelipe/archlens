package io.archlens.api.resource;

import io.archlens.application.service.AssessmentService;
import io.archlens.application.service.OrganizationService;
import io.archlens.infrastructure.persistence.InventoryEndpointEntity;
import io.archlens.infrastructure.persistence.InventoryMessagingEntity;
import io.archlens.infrastructure.persistence.InventoryModuleEntity;
import io.archlens.infrastructure.persistence.InventoryPackageEntity;
import io.archlens.infrastructure.persistence.InventoryPersistenceEntity;
import io.archlens.infrastructure.persistence.InventoryTypeEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/assessments/{assessmentId}/inventory")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Inventory")
public class InventoryResource {

    private static final int MAX_PAGE_SIZE = 1000;

    @Inject
    AssessmentService assessmentService;
    @Inject
    OrganizationService organizationService;

    @GET
    public Map<String, Object> get(
            @PathParam("assessmentId") UUID assessmentId,
            @QueryParam("offset") @DefaultValue("0") int offset,
            @QueryParam("limit") @DefaultValue("500") int limit
    ) {
        assessmentService.get(assessmentId);
        UUID organizationId = organizationService.requireOrganizationId();
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.min(MAX_PAGE_SIZE, Math.max(1, limit));
        int lastIndex = safeOffset + safeLimit - 1;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("offset", safeOffset);
        out.put("limit", safeLimit);

        List<InventoryModuleEntity> modules = InventoryModuleEntity
                .find("organizationId = ?1 and assessmentId = ?2 order by path, name", organizationId, assessmentId)
                .range(safeOffset, lastIndex)
                .list();
        out.put("modules", modules.stream().map(m -> Map.of(
                "name", nullToEmpty(m.name),
                "path", nullToEmpty(m.path),
                "buildSystem", m.buildSystem == null ? "UNKNOWN" : m.buildSystem
        )).toList());

        List<InventoryPackageEntity> packages = InventoryPackageEntity
                .find("organizationId = ?1 and assessmentId = ?2 order by name", organizationId, assessmentId)
                .range(safeOffset, lastIndex)
                .list();
        out.put("packages", packages.stream().map(p -> Map.of(
                "name", nullToEmpty(p.name),
                "fileCount", p.fileCount,
                "lineCount", p.lineCount
        )).toList());

        List<InventoryTypeEntity> types = InventoryTypeEntity
                .find("organizationId = ?1 and assessmentId = ?2 order by fqn", organizationId, assessmentId)
                .range(safeOffset, lastIndex)
                .list();
        out.put("types", types.stream().map(t -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("fqn", t.fqn);
            row.put("kind", t.kind);
            row.put("packageName", t.packageName);
            row.put("path", t.path);
            row.put("lineCount", t.lineCount);
            return row;
        }).toList());

        List<InventoryEndpointEntity> endpoints = InventoryEndpointEntity
                .find("organizationId = ?1 and assessmentId = ?2 order by sourcePath", organizationId, assessmentId)
                .range(safeOffset, lastIndex)
                .list();
        out.put("endpoints", endpoints.stream().map(e -> Map.of(
                "type", nullToEmpty(e.type),
                "pathOrName", nullToEmpty(e.pathOrName),
                "sourcePath", nullToEmpty(e.sourcePath),
                "evidenceId", nullToEmpty(e.evidenceId)
        )).toList());

        List<InventoryPersistenceEntity> persistence = InventoryPersistenceEntity
                .find("organizationId = ?1 and assessmentId = ?2 order by sourcePath", organizationId, assessmentId)
                .range(safeOffset, lastIndex)
                .list();
        out.put("persistence", persistence.stream().map(e -> Map.of(
                "technology", nullToEmpty(e.technology),
                "name", nullToEmpty(e.name),
                "sourcePath", nullToEmpty(e.sourcePath),
                "evidenceId", nullToEmpty(e.evidenceId)
        )).toList());

        List<InventoryMessagingEntity> messaging = InventoryMessagingEntity
                .find("organizationId = ?1 and assessmentId = ?2 order by sourcePath", organizationId, assessmentId)
                .range(safeOffset, lastIndex)
                .list();
        out.put("messaging", messaging.stream().map(e -> Map.of(
                "technology", nullToEmpty(e.technology),
                "name", nullToEmpty(e.name),
                "sourcePath", nullToEmpty(e.sourcePath),
                "evidenceId", nullToEmpty(e.evidenceId)
        )).toList());

        return out;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
