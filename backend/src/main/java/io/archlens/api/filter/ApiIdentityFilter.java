package io.archlens.api.filter;

import io.archlens.infrastructure.security.IdentityService;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

/**
 * Toda rota de produto exige identidade. Health público fica em {@code /api/v1/health}
 * e a prontidão do processo em {@code /q/health}.
 */
@Provider
public class ApiIdentityFilter implements ContainerRequestFilter {

    @Inject
    IdentityService identityService;

    @Override
    public void filter(ContainerRequestContext requestContext) {
        String path = requestContext.getUriInfo().getPath();
        if (path == null) {
            return;
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (!path.startsWith("api/v1") || path.equals("api/v1/health") || path.startsWith("api/v1/health/")) {
            return;
        }
        identityService.current();
    }
}
