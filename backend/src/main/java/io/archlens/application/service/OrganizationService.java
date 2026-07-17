package io.archlens.application.service;

import io.archlens.infrastructure.persistence.OrganizationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolução da organização corrente.
 * <p>
 * Política multi-tenant: se {@code X-Organization-Id} estiver presente, usa essa organização;
 * caso contrário, usa a organização única da instalação (bootstrap). Acesso cruzado por UUID
 * de recursos de outra organização responde {@code 404} (sem revelar existência).
 */
@ApplicationScoped
public class OrganizationService {

    @Context
    HttpHeaders headers;

    public OrganizationEntity getOrganization() {
        Optional<UUID> headerOrg = resolveHeaderOrgId();
        if (headerOrg.isPresent()) {
            OrganizationEntity org = OrganizationEntity.findById(headerOrg.get());
            if (org == null) {
                throw new NotFoundException("Organização não encontrada");
            }
            return org;
        }
        OrganizationEntity org = OrganizationEntity.findSingle();
        if (org == null) {
            throw new NotFoundException("Organização não configurada");
        }
        return org;
    }

    public UUID requireOrganizationId() {
        return getOrganization().id;
    }

    private Optional<UUID> resolveHeaderOrgId() {
        if (headers == null) {
            return Optional.empty();
        }
        String raw = headers.getHeaderString("X-Organization-Id");
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(raw.trim()));
        } catch (IllegalArgumentException e) {
            throw new NotFoundException("Organização não encontrada");
        }
    }
}
