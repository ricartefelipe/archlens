package io.archlens.api.resource;

import io.archlens.api.dto.CommonDtos.ImportResponse;
import io.archlens.application.service.AnalysisImportService;
import io.archlens.infrastructure.persistence.FindingEntity;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/assessments/{assessmentId}/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Analysis Import")
public class AnalysisImportResource {

    @Inject
    AnalysisImportService importService;

    @POST
    public Response importAnalysis(
            @PathParam("assessmentId") UUID assessmentId,
            @HeaderParam("Idempotency-Key") String idempotencyKey,
            String body) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Header Idempotency-Key é obrigatório");
        }
        var result = importService.importAnalysis(assessmentId, idempotencyKey.trim(), body);
        int count = FindingEntity.listByAssessment(assessmentId).size();
        return Response.ok(new ImportResponse(result.id, result.contentHash, result.importedAt, count)).build();
    }
}
