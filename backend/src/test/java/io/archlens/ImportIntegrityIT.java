package io.archlens;

import io.archlens.infrastructure.persistence.OrganizationEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ImportIntegrityIT {

    static final String DEMO_JSON;

    static {
        try {
            DEMO_JSON = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String assessmentId;

    @Test
    @Order(1)
    void sameContentDoesNotCreateASecondImport() {
        assessmentId = createAssessment("Hash dedup");
        String first = importJson(assessmentId, "key-a", DEMO_JSON);
        String second = importJson(assessmentId, "key-b", DEMO_JSON);
        assertEquals(first, second);
    }

    @Test
    @Order(2)
    void tamperedHashIsRejected() {
        String tampered = DEMO_JSON.replaceFirst(
                "\"contentHash\" : \"sha256:[0-9a-f]+\"",
                "\"contentHash\" : \"sha256:" + "ab".repeat(32) + "\""
        );
        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "tampered")
                .contentType(ContentType.JSON)
                .body(tampered)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(400)
                .body("detail", containsString("contentHash"));
    }

    @Test
    @Order(3)
    void anotherAssessmentKeepsItsOwnImport() {
        String other = createAssessment("Outro artefato");
        String imported = importJson(other, "key-other", DEMO_JSON);
        String original = importJson(assessmentId, "key-a", DEMO_JSON);
        assertNotEquals(imported, original);
    }

    private static String createAssessment(String name) {
        return given()
                .header("X-User", "tester")
                .header("X-Roles", "ADMIN")
                .contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"description\":\"integridade\"}")
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    private static String importJson(String assessment, String key, String json) {
        return given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", key)
                .contentType(ContentType.JSON)
                .body(json)
        .when()
                .post("/api/v1/assessments/" + assessment + "/import")
        .then()
                .statusCode(200)
                .extract().path("importId");
    }

    @Test
    @Order(4)
    void distinctOrganizationsKeepSeparateImportsOfTheSameHash() {
        UUID orgA = upsertOrg("Import Org A");
        UUID orgB = upsertOrg("Import Org B");
        String assessmentA = createAssessmentInOrg(orgA, "Artefato A");
        String assessmentB = createAssessmentInOrg(orgB, "Artefato B");
        String importA = importJsonInOrg(orgA, assessmentA, "org-a-key", DEMO_JSON);
        String importB = importJsonInOrg(orgB, assessmentB, "org-b-key", DEMO_JSON);
        assertNotEquals(importA, importB);

        given()
                .header("X-User", "intruder")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", orgB.toString())
        .when()
                .get("/api/v1/assessments/" + assessmentA)
        .then()
                .statusCode(404);
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

    private static String createAssessmentInOrg(UUID orgId, String name) {
        return given()
                .header("X-User", "tester")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", orgId.toString())
                .contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"description\":\"org\"}")
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .extract().path("id");
    }

    private static String importJsonInOrg(UUID orgId, String assessment, String key, String json) {
        return given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("X-Organization-Id", orgId.toString())
                .header("Idempotency-Key", key)
                .contentType(ContentType.JSON)
                .body(json)
        .when()
                .post("/api/v1/assessments/" + assessment + "/import")
        .then()
                .statusCode(200)
                .extract().path("importId");
    }
}
