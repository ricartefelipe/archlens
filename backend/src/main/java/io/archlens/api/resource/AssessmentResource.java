package io.archlens.api.resource;

import io.archlens.api.dto.AssessmentDtos.*;
import io.archlens.application.service.AssessmentService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/assessments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Assessments")
public class AssessmentResource {

    @Inject
    AssessmentService assessmentService;

    @GET
    public List<AssessmentResponse> list() {
        return assessmentService.list();
    }

    @GET
    @Path("/{id}")
    public AssessmentResponse get(@PathParam("id") UUID id) {
        return assessmentService.get(id);
    }

    @POST
    public Response create(@Valid CreateAssessmentRequest req) {
        return Response.status(Response.Status.CREATED).entity(assessmentService.create(req)).build();
    }

    @PUT
    @Path("/{id}")
    public AssessmentResponse update(@PathParam("id") UUID id, @Valid UpdateAssessmentRequest req) {
        return assessmentService.update(id, req);
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        assessmentService.delete(id);
        return Response.noContent().build();
    }
}
