package io.archlens.api.resource;

import io.archlens.api.dto.CommonDtos.*;
import io.archlens.application.service.ModernizationService;
import io.archlens.report.ReportService;
import io.archlens.infrastructure.persistence.ReportSnapshotEntity;
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
@Tag(name = "Modernization & Reports")
public class ModernizationResource {

    @Inject
    ModernizationService modernizationService;
    @Inject
    ReportService reportService;

    @GET
    @Path("/modernization-options")
    public List<ModernizationOptionResponse> listOptions(@PathParam("assessmentId") UUID assessmentId) {
        return modernizationService.listOptions(assessmentId);
    }

    @POST
    @Path("/modernization-options")
    public Response createOption(@PathParam("assessmentId") UUID assessmentId, @Valid ModernizationOptionRequest req) {
        return Response.status(Response.Status.CREATED)
                .entity(modernizationService.createOption(assessmentId, req)).build();
    }

    @PUT
    @Path("/modernization-options/{optionId}")
    public ModernizationOptionResponse updateOption(
            @PathParam("assessmentId") UUID assessmentId,
            @PathParam("optionId") UUID optionId,
            @Valid ModernizationOptionRequest req) {
        return modernizationService.updateOption(assessmentId, optionId, req);
    }

    @DELETE
    @Path("/modernization-options/{optionId}")
    public Response deleteOption(@PathParam("assessmentId") UUID assessmentId, @PathParam("optionId") UUID optionId) {
        modernizationService.deleteOption(assessmentId, optionId);
        return Response.noContent().build();
    }

    @GET
    @Path("/roadmap")
    public List<RoadmapItemResponse> listRoadmap(@PathParam("assessmentId") UUID assessmentId) {
        return modernizationService.listRoadmap(assessmentId);
    }

    @POST
    @Path("/roadmap")
    public Response createRoadmap(@PathParam("assessmentId") UUID assessmentId, @Valid RoadmapItemRequest req) {
        return Response.status(Response.Status.CREATED)
                .entity(modernizationService.createRoadmap(assessmentId, req)).build();
    }

    @PUT
    @Path("/roadmap/{itemId}")
    public RoadmapItemResponse updateRoadmap(
            @PathParam("assessmentId") UUID assessmentId,
            @PathParam("itemId") UUID itemId,
            @Valid RoadmapItemRequest req) {
        return modernizationService.updateRoadmap(assessmentId, itemId, req);
    }

    @DELETE
    @Path("/roadmap/{itemId}")
    public Response deleteRoadmap(@PathParam("assessmentId") UUID assessmentId, @PathParam("itemId") UUID itemId) {
        modernizationService.deleteRoadmap(assessmentId, itemId);
        return Response.noContent().build();
    }

    @POST
    @Path("/reports/generate")
    public ReportSnapshotResponse generate(@PathParam("assessmentId") UUID assessmentId) {
        ReportSnapshotEntity snap = reportService.generate(assessmentId);
        return toResponse(snap);
    }

    @GET
    @Path("/reports")
    public List<ReportSnapshotResponse> listReports(@PathParam("assessmentId") UUID assessmentId) {
        return reportService.list(assessmentId).stream().map(this::toResponse).toList();
    }

    @POST
    @Path("/reports/{snapshotId}/publish")
    public ReportSnapshotResponse publish(
            @PathParam("assessmentId") UUID assessmentId,
            @PathParam("snapshotId") UUID snapshotId) {
        return toResponse(reportService.publish(assessmentId, snapshotId));
    }

    @GET
    @Path("/reports/{snapshotId}/html")
    @Produces(MediaType.TEXT_HTML)
    public Response downloadHtml(
            @PathParam("assessmentId") UUID assessmentId,
            @PathParam("snapshotId") UUID snapshotId) {
        ReportSnapshotEntity snap = reportService.getSnapshot(assessmentId, snapshotId);
        return Response.ok(snap.htmlContent).type(MediaType.TEXT_HTML).build();
    }

    @GET
    @Path("/reports/{snapshotId}/pdf")
    @Produces("application/pdf")
    public Response downloadPdf(
            @PathParam("assessmentId") UUID assessmentId,
            @PathParam("snapshotId") UUID snapshotId) {
        ReportSnapshotEntity snap = reportService.getSnapshot(assessmentId, snapshotId);
        if (snap.pdfContent == null || snap.pdfContent.length == 0) {
            throw new NotFoundException("PDF não disponível");
        }
        return Response.ok(snap.pdfContent).type("application/pdf")
                .header("Content-Disposition", "attachment; filename=\"report-v" + snap.version + ".pdf\"")
                .build();
    }

    private ReportSnapshotResponse toResponse(ReportSnapshotEntity snap) {
        return new ReportSnapshotResponse(snap.id, snap.version, snap.contentHash, snap.published,
                snap.publishedAt, snap.publishedBy, snap.createdAt);
    }
}
