package io.archlens.api.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Provider
public class SecurityHeadersFilter implements ContainerResponseFilter {

    @ConfigProperty(name = "archlens.security.hsts.enabled", defaultValue = "false")
    boolean hstsEnabled;

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) {
        var headers = responseContext.getHeaders();
        headers.putSingle("X-Content-Type-Options", "nosniff");
        headers.putSingle("X-Frame-Options", "DENY");
        headers.putSingle("Referrer-Policy", "no-referrer");
        headers.putSingle("Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'");
        headers.putSingle("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        if (hstsEnabled) {
            headers.putSingle("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
    }
}
