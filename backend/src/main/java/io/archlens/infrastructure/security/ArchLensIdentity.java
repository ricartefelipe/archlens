package io.archlens.infrastructure.security;

import io.archlens.domain.UserRole;
import java.util.Set;

public record ArchLensIdentity(String subject, String email, Set<UserRole> roles) {
    public boolean hasRole(UserRole role) {
        return roles.contains(role);
    }

    public boolean canWrite() {
        return roles.stream().anyMatch(UserRole::canWrite);
    }
}
