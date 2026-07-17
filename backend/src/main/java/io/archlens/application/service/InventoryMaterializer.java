package io.archlens.application.service;

import io.archlens.application.model.AnalysisDocument;
import io.archlens.infrastructure.persistence.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class InventoryMaterializer {

    @Transactional
    public void materialize(UUID organizationId, UUID assessmentId, AnalysisDocument doc) {
        clearInventory(organizationId, assessmentId);
        var inv = doc.inventory();

        if (inv != null && inv.modules() != null) {
            for (var m : inv.modules()) {
                var e = new InventoryModuleEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.name = m.name();
                e.path = m.path();
                e.buildSystem = m.buildSystem();
                e.persist();
            }
        }
        if (inv != null && inv.packages() != null) {
            for (var p : inv.packages()) {
                var e = new InventoryPackageEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.name = p.name();
                e.fileCount = p.fileCount();
                e.lineCount = p.lineCount();
                e.persist();
            }
        }
        if (inv != null && inv.types() != null) {
            for (var t : inv.types()) {
                var e = new InventoryTypeEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.fqn = t.fqn();
                e.kind = t.kind();
                e.packageName = t.packageName();
                e.path = t.path();
                e.lineCount = t.lineCount();
                e.annotations = t.annotations() != null ? String.join(",", t.annotations()) : null;
                e.persist();
            }
        }
        if (inv != null && inv.endpoints() != null) {
            for (var ep : inv.endpoints()) {
                var e = new InventoryEndpointEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.type = ep.type();
                e.pathOrName = ep.pathOrName();
                e.sourcePath = ep.sourcePath();
                e.evidenceId = ep.evidenceId();
                e.persist();
            }
        }
        if (inv != null && inv.persistence() != null) {
            for (var pe : inv.persistence()) {
                var e = new InventoryPersistenceEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.technology = pe.technology();
                e.name = pe.name();
                e.sourcePath = pe.sourcePath();
                e.evidenceId = pe.evidenceId();
                e.persist();
            }
        }
        if (inv != null && inv.messaging() != null) {
            for (var me : inv.messaging()) {
                var e = new InventoryMessagingEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.technology = me.technology();
                e.name = me.name();
                e.sourcePath = me.sourcePath();
                e.evidenceId = me.evidenceId();
                e.persist();
            }
        }
        if (doc.signals() != null) {
            for (var s : doc.signals()) {
                var e = new InventorySignalEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.signalId = s.id();
                e.category = s.category();
                e.name = s.name();
                e.confidence = s.confidence();
                e.evidenceIds = s.evidenceIds() != null ? String.join(",", s.evidenceIds()) : "";
                e.persist();
            }
        }
        if (doc.dependencies() != null) {
            for (var d : doc.dependencies()) {
                var e = new InventoryDependencyEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.coordinates = d.coordinates();
                e.scope = d.scope();
                e.sourcePath = d.sourcePath();
                e.persist();
            }
        }
        if (doc.hotspots() != null) {
            for (var h : doc.hotspots()) {
                var e = new InventoryHotspotEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.path = h.path();
                e.lineCount = h.lineCount();
                e.reason = h.reason();
                e.evidenceId = h.evidenceId();
                e.persist();
            }
        }
        if (doc.cycles() != null) {
            for (var c : doc.cycles()) {
                var e = new InventoryCycleEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.packages = c.packages() != null ? String.join(",", c.packages()) : "";
                e.evidenceIds = c.evidenceIds() != null ? String.join(",", c.evidenceIds()) : "";
                e.persist();
            }
        }
        if (doc.secrets() != null) {
            for (var s : doc.secrets()) {
                var e = new InventorySecretEntity();
                e.id = UUID.randomUUID();
                e.organizationId = organizationId;
                e.assessmentId = assessmentId;
                e.type = s.type();
                e.path = s.path();
                e.line = s.line();
                e.valueHash = s.valueHash();
                e.persist();
            }
        }
        if (doc.metrics() != null) {
            var m = doc.metrics();
            var e = new InventoryMetricsEntity();
            e.id = UUID.randomUUID();
            e.organizationId = organizationId;
            e.assessmentId = assessmentId;
            e.fileCount = m.fileCount();
            e.javaFileCount = m.javaFileCount();
            e.lineCount = m.lineCount();
            e.typeCount = m.typeCount();
            e.testFileCount = m.testFileCount();
            e.dependencyCount = m.dependencyCount();
            e.persist();
        }
    }

    private void clearInventory(UUID organizationId, UUID assessmentId) {
        InventoryModuleEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventoryPackageEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventoryTypeEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventoryEndpointEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventoryPersistenceEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventoryMessagingEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventorySignalEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventoryDependencyEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventoryHotspotEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventoryCycleEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventorySecretEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
        InventoryMetricsEntity.delete("organizationId = ?1 and assessmentId = ?2", organizationId, assessmentId);
    }
}
