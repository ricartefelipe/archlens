package io.archlens.api.resource;

import io.archlens.application.service.AssessmentService;
import io.archlens.application.service.OrganizationService;
import io.archlens.infrastructure.persistence.AssessmentEntity;
import io.archlens.infrastructure.persistence.FindingEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.LinkedHashMap;
import java.util.Map;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/organization")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Organization")
public class OrganizationResource {

    @Inject
    OrganizationService organizationService;

    @GET
    @Path("/summary")
    public Map<String, Object> summary() {
        var org = organizationService.getOrganization();
        long assessments = AssessmentEntity.count("organizationId", org.id);
        long findings = FindingEntity.count("organizationId", org.id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", org.id);
        out.put("name", org.name);
        out.put("assessmentCount", assessments);
        out.put("findingCount", findings);
        return out;
    }
}
