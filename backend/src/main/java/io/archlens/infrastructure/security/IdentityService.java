package io.archlens.infrastructure.security;

import io.archlens.domain.UserRole;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.oidc.UserInfo;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class IdentityService {
    @Inject
    SecurityIdentity securityIdentity;

    @Context
    HttpHeaders headers;

    @ConfigProperty(name = "quarkus.oidc.enabled", defaultValue = "true")
    boolean oidcEnabled;

    public ArchLensIdentity current() {
        if (!oidcEnabled) {
            String user = Optional.ofNullable(headers.getHeaderString("X-User")).orElse("dev-user");
            String rolesHeader = Optional.ofNullable(headers.getHeaderString("X-Roles")).orElse("ADMIN");
            Set<UserRole> roles = parseRoles(rolesHeader);
            return new ArchLensIdentity(user, user + "@local", roles);
        }
        String subject = securityIdentity.getPrincipal().getName();
        Set<UserRole> roles = securityIdentity.getRoles().stream()
                .map(this::mapRole)
                .filter(r -> r != null)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
        if (roles.isEmpty()) {
            roles.add(UserRole.VIEWER);
        }
        return new ArchLensIdentity(subject, subject, roles);
    }

    private UserRole mapRole(String role) {
        try {
            return UserRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Set<UserRole> parseRoles(String header) {
        return Arrays.stream(header.split(","))
                .map(String::trim)
                .map(String::toUpperCase)
                .map(r -> {
                    try { return UserRole.valueOf(r); } catch (Exception e) { return null; }
                })
                .filter(r -> r != null)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(UserRole.class)));
    }

    public void requireWrite() {
        if (!current().canWrite()) {
            throw new jakarta.ws.rs.ForbiddenException("Permissão insuficiente");
        }
    }

    public void requireRole(UserRole role) {
        if (!current().hasRole(role) && !current().hasRole(UserRole.ADMIN)) {
            throw new jakarta.ws.rs.ForbiddenException("Papel requerido: " + role);
        }
    }
}
