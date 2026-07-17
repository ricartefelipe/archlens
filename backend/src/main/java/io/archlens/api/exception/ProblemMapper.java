package io.archlens.api.exception;

import io.archlens.api.filter.CorrelationIdFilter;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Provider
public class ProblemMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(ProblemMapper.class);

    @Context
    UriInfo uriInfo;

    @Context
    ContainerRequestContext requestContext;

    @Override
    public Response toResponse(Throwable ex) {
        int status = 500;
        String title = "Erro interno";
        String detail = "Ocorreu um erro inesperado";

        if (ex instanceof NotFoundException) {
            status = 404;
            title = "Não encontrado";
            detail = safeDetail(ex.getMessage(), "Recurso não encontrado");
        } else if (ex instanceof ConflictException) {
            status = 409;
            title = "Conflito";
            detail = safeDetail(ex.getMessage(), "Conflito de estado");
        } else if (ex instanceof PayloadTooLargeException) {
            status = 413;
            title = "Payload muito grande";
            detail = safeDetail(ex.getMessage(), "Payload excede o limite configurado");
        } else if (ex instanceof BadRequestException) {
            status = 400;
            title = "Requisição inválida";
            detail = safeDetail(ex.getMessage(), "Requisição inválida");
        } else if (ex instanceof ForbiddenException) {
            status = 403;
            title = "Acesso negado";
            detail = safeDetail(ex.getMessage(), "Acesso negado");
        } else if (ex instanceof NotAuthorizedException) {
            status = 401;
            title = "Não autenticado";
            detail = "Autenticação necessária";
        } else if (ex instanceof NotSupportedException) {
            status = 415;
            title = "Tipo de mídia não suportado";
            detail = "Content-Type deve ser application/json quando aplicável";
        } else if (ex instanceof ConstraintViolationException cve) {
            status = 400;
            title = "Validação falhou";
            detail = cve.getConstraintViolations().stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining("; "));
        } else if (ex instanceof WebApplicationException wae) {
            status = wae.getResponse().getStatus();
            Response.Status statusEnum = Response.Status.fromStatusCode(status);
            title = statusEnum != null ? statusEnum.getReasonPhrase() : "Erro";
            detail = safeDetail(ex.getMessage(), title);
        }

        String correlationId = resolveCorrelationId();
        if (status >= 500) {
            LOG.errorf(ex, "Erro interno correlationId=%s path=%s", correlationId,
                    uriInfo != null ? uriInfo.getPath() : "/");
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", title);
        body.put("status", status);
        body.put("detail", detail);
        body.put("instance", uriInfo != null ? uriInfo.getPath() : "/");
        body.put("correlationId", correlationId);

        return Response.status(status)
                .type("application/problem+json")
                .header(CorrelationIdFilter.HEADER, correlationId)
                .entity(body)
                .build();
    }

    private String resolveCorrelationId() {
        if (requestContext != null) {
            Object existing = requestContext.getProperty(CorrelationIdFilter.PROPERTY);
            if (existing != null) {
                return existing.toString();
            }
            String header = requestContext.getHeaderString(CorrelationIdFilter.HEADER);
            if (header != null && !header.isBlank()) {
                return header.trim();
            }
        }
        return UUID.randomUUID().toString();
    }

    private static String safeDetail(String message, String fallback) {
        if (message == null || message.isBlank()) {
            return fallback;
        }
        String lower = message.toLowerCase();
        if (lower.contains("sql") || lower.contains("jdbc") || lower.contains("hibernate")
                || lower.contains("exception") || message.length() > 500) {
            return fallback;
        }
        return message;
    }
}
