package io.archlens.api.resource;

import io.archlens.api.dto.CommonDtos.AuditEventResponse;
import io.archlens.application.service.OrganizationService;
import io.archlens.infrastructure.persistence.AuditEventEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/audit")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Audit")
public class AuditResource {

    @Inject
    OrganizationService organizationService;

    @GET
    public List<AuditEventResponse> list(@QueryParam("limit") @DefaultValue("100") int limit) {
        var org = organizationService.getOrganization();
        return AuditEventEntity.listByOrg(org.id, Math.min(limit, 500)).stream()
                .map(e -> new AuditEventResponse(e.id, e.assessmentId, e.action.name(), e.actor, e.details, e.occurredAt))
                .toList();
    }
}
