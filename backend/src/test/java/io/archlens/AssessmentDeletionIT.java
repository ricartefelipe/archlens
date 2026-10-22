package io.archlens;

import io.archlens.application.service.BlobCleanupService;
import io.archlens.infrastructure.persistence.AnalysisImportEntity;
import io.archlens.infrastructure.persistence.BlobCleanupTaskEntity;
import io.archlens.infrastructure.persistence.FindingEntity;
import io.archlens.infrastructure.persistence.InventoryModuleEntity;
import io.archlens.infrastructure.persistence.OrganizationEntity;
import io.archlens.infrastructure.persistence.ReportSnapshotEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class AssessmentDeletionIT {

    @Test
    void deleteRemovesDependentsAndDownloadAndCleanupCanRunAgain() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        String assessmentId = given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Exclusão\",\"description\":\"purge\"}")
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .extract().path("id");
        assertNotNull(assessmentId);

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "purge-import")
                .contentType(ContentType.JSON)
                .body(json)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(200);

        String snapshotId = given()
                .header("X-User", "admin")
                .header("X-Roles", "ARCHITECT")
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/reports/generate")
        .then()
                .statusCode(200)
                .extract().path("id");

        given()
                .header("X-User", "admin")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/reports/" + snapshotId + "/pdf")
        .then()
                .statusCode(200);

        given()
                .header("X-User", "viewer")
                .header("X-Roles", "VIEWER")
        .when()
                .delete("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(403);

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .delete("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(204);

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .get("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(404);

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/reports/" + snapshotId + "/pdf")
        .then()
                .statusCode(404);

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/findings")
        .then()
                .statusCode(404);

        given()
                .header("X-User", "admin")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/audit?limit=200")
        .then()
                .statusCode(200)
                .body("action", hasItem("ASSESSMENT_DELETED"));

        UUID removed = UUID.fromString(assessmentId);
        UUID snapshot = UUID.fromString(snapshotId);
        QuarkusTransaction.requiringNew().run(() -> {
            assertEquals(0, FindingEntity.count("assessmentId", removed));
            assertEquals(0, AnalysisImportEntity.count("assessmentId", removed));
            assertEquals(0, InventoryModuleEntity.count("assessmentId", removed));
            ReportSnapshotEntity stored = ReportSnapshotEntity.findById(snapshot);
            assertNotNull(stored);
            assertNull(stored.assessmentId);
            assertNull(stored.pdfContent);
            assertFalse(stored.published);
            assertEquals(1, BlobCleanupTaskEntity.count("assessmentId = ?1 and status = ?2", removed, "PENDING"));
        });

        given()
                .header("X-User", "viewer")
                .header("X-Roles", "VIEWER")
        .when()
                .post("/api/v1/maintenance/blob-cleanup")
        .then()
                .statusCode(403);

        BlobCleanupService.failNextStorageAttempts(1);
        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .post("/api/v1/maintenance/blob-cleanup")
        .then()
                .statusCode(200)
                .body("completed", equalTo(0));

        long stillPending = QuarkusTransaction.requiringNew().call(() ->
                BlobCleanupTaskEntity.count("assessmentId = ?1 and status = ?2", removed, "PENDING"));
        assertEquals(1, stillPending);

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .post("/api/v1/maintenance/blob-cleanup")
        .then()
                .statusCode(200)
                .body("completed", greaterThanOrEqualTo(1));

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .post("/api/v1/maintenance/blob-cleanup")
        .then()
                .statusCode(200)
                .body("completed", equalTo(0));

        QuarkusTransaction.requiringNew().run(() ->
                assertNull(ReportSnapshotEntity.findById(snapshot)));
    }

    @Test
    void otherOrganizationCannotDeleteOrRead() {
        UUID orgA = upsertOrg("Delete Org A");
        UUID orgB = upsertOrg("Delete Org B");
        String assessmentA = given()
                .header("X-User", "admin-a")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", orgA.toString())
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Org A\",\"description\":\"isolamento\"}")
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("X-User", "admin-b")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", orgB.toString())
        .when()
                .delete("/api/v1/assessments/" + assessmentA)
        .then()
                .statusCode(404);

        given()
                .header("X-User", "admin-b")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", orgB.toString())
        .when()
                .get("/api/v1/assessments/" + assessmentA + "/reports/00000000-0000-4000-8000-000000000001/pdf")
        .then()
                .statusCode(404);

        given()
                .header("X-User", "admin-a")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", orgA.toString())
        .when()
                .get("/api/v1/assessments/" + assessmentA)
        .then()
                .statusCode(200);
    }

    @Test
    void failedAuditInsertRollsBackTheDelete() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        String assessmentId = given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Rollback\",\"description\":\"tx\"}")
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "rollback-import")
                .contentType(ContentType.JSON)
                .body(json)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(200);

        given()
                .header("X-User", "a".repeat(300))
                .header("X-Roles", "ADMIN")
        .when()
                .delete("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(500);

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .get("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(200);

        long findings = QuarkusTransaction.requiringNew().call(() ->
                FindingEntity.count("assessmentId", UUID.fromString(assessmentId)));
        assertEquals(true, findings > 0);
    }

    private static UUID upsertOrg(String name) {
        return QuarkusTransaction.requiringNew().call(() -> {
            OrganizationEntity existing = OrganizationEntity.find("name", name).firstResult();
            if (existing != null) {
                return existing.id;
            }
            OrganizationEntity org = new OrganizationEntity();
            org.id = UUID.randomUUID();
            org.name = name;
            org.createdAt = Instant.now();
            org.persist();
            return org.id;
        });
    }
}
