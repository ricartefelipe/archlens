package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organization")
public class OrganizationEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    public String name;
    @Column(name = "created_at")
    public Instant createdAt;

    public static OrganizationEntity findSingle() {
        return findAll().firstResult();
    }
}
