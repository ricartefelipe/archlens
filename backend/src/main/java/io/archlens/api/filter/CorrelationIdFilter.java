package io.archlens.api.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.UUID;
import org.jboss.logging.MDC;

@Provider
public class CorrelationIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String PROPERTY = "archlens.correlationId";
    public static final String MDC_KEY = "correlationId";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String incoming = requestContext.getHeaderString(HEADER);
        String correlationId = (incoming == null || incoming.isBlank())
                ? UUID.randomUUID().toString()
                : incoming.trim();
        requestContext.setProperty(PROPERTY, correlationId);
        MDC.put(MDC_KEY, correlationId);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object correlationId = requestContext.getProperty(PROPERTY);
        if (correlationId != null) {
            responseContext.getHeaders().putSingle(HEADER, correlationId.toString());
        }
        MDC.remove(MDC_KEY);
    }
}
