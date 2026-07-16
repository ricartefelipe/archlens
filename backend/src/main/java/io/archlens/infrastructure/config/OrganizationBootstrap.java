package io.archlens.infrastructure.config;

import io.archlens.infrastructure.persistence.OrganizationEntity;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class OrganizationBootstrap {
    @ConfigProperty(name = "archlens.organization.name", defaultValue = "ArchLens")
    String orgName;

    @Transactional
    void onStart(@Observes StartupEvent event) {
        if (OrganizationEntity.count() == 0) {
            OrganizationEntity org = new OrganizationEntity();
            org.id = UUID.randomUUID();
            org.name = orgName;
            org.createdAt = Instant.now();
            org.persist();
        }
    }
}
