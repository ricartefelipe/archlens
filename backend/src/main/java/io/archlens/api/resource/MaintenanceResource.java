package io.archlens.api.resource;

import io.archlens.application.service.BlobCleanupService;
import io.archlens.infrastructure.security.IdentityService;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/maintenance")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Maintenance")
public class MaintenanceResource {

    @Inject
    IdentityService identityService;
    @Inject
    BlobCleanupService blobCleanupService;

    @POST
    @Path("/blob-cleanup")
    public Map<String, Integer> cleanupBlobs() {
        identityService.requireRole(io.archlens.domain.UserRole.ADMIN);
        return Map.of("completed", blobCleanupService.sweep());
    }
}
