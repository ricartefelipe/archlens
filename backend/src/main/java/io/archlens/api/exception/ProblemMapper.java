package io.archlens.api.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Provider
public class ProblemMapper implements ExceptionMapper<Throwable> {
    @Context UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable ex) {
        int status = 500;
        String title = "Erro interno";
        String detail = "Ocorreu um erro inesperado";

        if (ex instanceof NotFoundException) {
            status = 404;
            title = "Não encontrado";
            detail = ex.getMessage();
        } else if (ex instanceof BadRequestException) {
            status = 400;
            title = "Requisição inválida";
            detail = ex.getMessage();
        } else if (ex instanceof ForbiddenException) {
            status = 403;
            title = "Acesso negado";
            detail = ex.getMessage();
        } else if (ex instanceof ConstraintViolationException cve) {
            status = 400;
            title = "Validação falhou";
            detail = cve.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
        } else if (ex instanceof WebApplicationException wae) {
            status = wae.getResponse().getStatus();
            title = Response.Status.fromStatusCode(status).getReasonPhrase();
            detail = ex.getMessage();
        }

        Map<String, Object> body = new HashMap<>();
        body.put("type", "about:blank");
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("instance", uriInfo != null ? uriInfo.getPath() : "/");

        return Response.status(status)
                .type("application/problem+json")
                .entity(body)
                .build();
    }
}
