package io.archlens.api.resource;

import io.archlens.application.service.AssessmentService;
import io.archlens.infrastructure.persistence.InventoryEndpointEntity;
import io.archlens.infrastructure.persistence.InventoryMessagingEntity;
import io.archlens.infrastructure.persistence.InventoryModuleEntity;
import io.archlens.infrastructure.persistence.InventoryPackageEntity;
import io.archlens.infrastructure.persistence.InventoryPersistenceEntity;
import io.archlens.infrastructure.persistence.InventoryTypeEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
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

    @Inject
    AssessmentService assessmentService;

    @GET
    public Map<String, Object> get(@PathParam("assessmentId") UUID assessmentId) {
        assessmentService.get(assessmentId);
        Map<String, Object> out = new LinkedHashMap<>();

        List<InventoryModuleEntity> modules = InventoryModuleEntity.list("assessmentId", assessmentId);
        out.put("modules", modules.stream().map(m -> Map.of(
                "name", nullToEmpty(m.name),
                "path", nullToEmpty(m.path),
                "buildSystem", m.buildSystem == null ? "UNKNOWN" : m.buildSystem
        )).toList());

        List<InventoryPackageEntity> packages = InventoryPackageEntity.list("assessmentId", assessmentId);
        out.put("packages", packages.stream().map(p -> Map.of(
                "name", nullToEmpty(p.name),
                "fileCount", p.fileCount,
                "lineCount", p.lineCount
        )).toList());

        List<InventoryTypeEntity> types = InventoryTypeEntity.list("assessmentId", assessmentId);
        out.put("types", types.stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("fqn", t.fqn);
            m.put("kind", t.kind);
            m.put("packageName", t.packageName);
            m.put("path", t.path);
            m.put("lineCount", t.lineCount);
            return m;
        }).toList());

        List<InventoryEndpointEntity> endpoints = InventoryEndpointEntity.list("assessmentId", assessmentId);
        out.put("endpoints", endpoints.stream().map(e -> Map.of(
                "type", nullToEmpty(e.type),
                "pathOrName", nullToEmpty(e.pathOrName),
                "sourcePath", nullToEmpty(e.sourcePath),
                "evidenceId", nullToEmpty(e.evidenceId)
        )).toList());

        List<InventoryPersistenceEntity> persistence = InventoryPersistenceEntity.list("assessmentId", assessmentId);
        out.put("persistence", persistence.stream().map(e -> Map.of(
                "technology", nullToEmpty(e.technology),
                "name", nullToEmpty(e.name),
                "sourcePath", nullToEmpty(e.sourcePath),
                "evidenceId", nullToEmpty(e.evidenceId)
        )).toList());

        List<InventoryMessagingEntity> messaging = InventoryMessagingEntity.list("assessmentId", assessmentId);
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
