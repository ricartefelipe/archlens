package io.archlens.api.resource;

import io.archlens.api.dto.CommonDtos.*;
import io.archlens.application.service.QuestionnaireService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/assessments/{assessmentId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Questionnaire & Dimensions")
public class QuestionnaireResource {

    @Inject
    QuestionnaireService questionnaireService;

    @GET
    @Path("/questionnaire")
    public QuestionnaireResponse getQuestionnaire() {
        return questionnaireService.getQuestionnaire();
    }

    @POST
    @Path("/questionnaire")
    public Response submit(
            @PathParam("assessmentId") UUID assessmentId,
            @Valid SubmitQuestionnaireRequest req) {
        questionnaireService.submit(assessmentId, req);
        return Response.noContent().build();
    }

    @GET
    @Path("/dimensions")
    public List<DimensionScoreResponse> getDimensions(@PathParam("assessmentId") UUID assessmentId) {
        return questionnaireService.listDimensions(assessmentId);
    }
}
