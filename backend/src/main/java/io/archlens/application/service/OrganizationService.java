package io.archlens.application.service;

import io.archlens.infrastructure.persistence.OrganizationEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.NotFoundException;

@ApplicationScoped
public class OrganizationService {
    public OrganizationEntity getOrganization() {
        OrganizationEntity org = OrganizationEntity.findSingle();
        if (org == null) {
            throw new NotFoundException("Organização não configurada");
        }
        return org;
    }
}
