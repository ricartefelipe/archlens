package io.archlens.api.resource;

import io.archlens.api.dto.FindingDtos.*;
import io.archlens.application.service.FindingService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/assessments/{assessmentId}/findings")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Findings")
public class FindingResource {

    @Inject
    FindingService findingService;

    @GET
    public List<FindingResponse> list(@PathParam("assessmentId") UUID assessmentId) {
        return findingService.list(assessmentId);
    }

    @GET
    @Path("/{findingId}")
    public FindingResponse get(@PathParam("assessmentId") UUID assessmentId, @PathParam("findingId") UUID findingId) {
        return findingService.get(assessmentId, findingId);
    }

    @PATCH
    @Path("/{findingId}")
    public FindingResponse patchStatus(
            @PathParam("assessmentId") UUID assessmentId,
            @PathParam("findingId") UUID findingId,
            @Valid UpdateFindingStatusRequest req) {
        return findingService.updateStatus(assessmentId, findingId, req);
    }

    @GET
    @Path("/export.csv")
    @Produces("text/csv")
    public Response exportCsv(@PathParam("assessmentId") UUID assessmentId) {
        List<FindingResponse> findings = findingService.list(assessmentId);
        StreamingOutput stream = out -> {
            out.write("ruleId,title,severity,status,evidenceIds\n".getBytes(StandardCharsets.UTF_8));
            for (FindingResponse f : findings) {
                String line = String.format("%s,\"%s\",%s,%s,\"%s\"%n",
                        f.ruleId(), f.title().replace("\"", "\"\""), f.severity(), f.status(),
                        String.join(";", f.evidenceIds()));
                out.write(line.getBytes(StandardCharsets.UTF_8));
            }
        };
        return Response.ok(stream)
                .header("Content-Disposition", "attachment; filename=\"findings.csv\"")
                .build();
    }
}
