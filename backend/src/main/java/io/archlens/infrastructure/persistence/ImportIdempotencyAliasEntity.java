package io.archlens.infrastructure.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "import_idempotency_alias")
public class ImportIdempotencyAliasEntity extends PanacheEntityBase {
    @Id
    @Column(columnDefinition = "uuid")
    public UUID id;
    @Column(name = "organization_id", columnDefinition = "uuid")
    public UUID organizationId;
    @Column(name = "assessment_id", columnDefinition = "uuid")
    public UUID assessmentId;
    @Column(name = "idempotency_key")
    public String idempotencyKey;
    @Column(name = "import_id", columnDefinition = "uuid")
    public UUID importId;

    public static ImportIdempotencyAliasEntity findKey(UUID orgId, UUID assessmentId, String key) {
        return find("organizationId = ?1 and assessmentId = ?2 and idempotencyKey = ?3",
                orgId, assessmentId, key).firstResult();
    }
}
