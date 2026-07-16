package io.archlens;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ArchLensApiIT {

    static String assessmentId;
    static String importId;
    static String findingId;
    static String snapshotId;

    static final String VALID_JSON;

    static {
        try {
            VALID_JSON = Files.readString(Path.of("src/test/resources/valid-analysis.json"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Order(1)
    void healthReportsRulesLoaded() {
        given()
                .header("X-User", "tester")
                .header("X-Roles", "ADMIN")
        .when()
                .get("/api/v1/health")
        .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("rulesLoaded", greaterThanOrEqualTo(25));
    }

    @Test
    @Order(2)
    void createAssessment() {
        assessmentId = given()
                .header("X-User", "tester")
                .header("X-Roles", "ADMIN")
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Demo Legacy", "description", "Test assessment"))
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .body("name", equalTo("Demo Legacy"))
                .extract().path("id");
        assertNotNull(assessmentId);
    }

    @Test
    @Order(3)
    void invalidImportRejected() {
        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(ContentType.JSON)
                .body("{\"invalid\":true}")
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(400)
                .contentType("application/problem+json");
    }

    @Test
    @Order(4)
    void validImportMaterializesAndGeneratesFindings() {
        String key = "import-key-1";
        importId = given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", key)
                .contentType(ContentType.JSON)
                .body(VALID_JSON)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(200)
                .body("findingsCount", greaterThan(0))
                .extract().path("importId");

        given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/findings")
        .then()
                .statusCode(200)
                .body("size()", greaterThan(0))
                .body("[0].evidenceIds", not(empty()));
    }

    @Test
    @Order(5)
    void importIsIdempotent() {
        String first = given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "idem-key-stable")
                .contentType(ContentType.JSON)
                .body(VALID_JSON)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(200)
                .extract().path("importId");

        String second = given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "idem-key-stable")
                .contentType(ContentType.JSON)
                .body(VALID_JSON)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(200)
                .extract().path("importId");

        assertEquals(first, second);
    }

    @Test
    @Order(6)
    void questionnaireChangesScores() {
        var before = given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/dimensions")
        .then()
                .statusCode(200)
                .extract().jsonPath().getDouble("find { it.dimensionKey == 'BUSINESS_CRITICALITY' }.score");

        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "answers", Map.of(
                                "business_criticality", 5,
                                "team_skill", 2,
                                "risk_tolerance", 4,
                                "change_urgency", 5,
                                "security_priority", 5
                        )
                ))
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/questionnaire")
        .then()
                .statusCode(204);

        var after = given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/dimensions")
        .then()
                .statusCode(200)
                .extract().jsonPath().getDouble("find { it.dimensionKey == 'BUSINESS_CRITICALITY' }.score");

        assertTrue(after > before, "Questionário deve aumentar BUSINESS_CRITICALITY");
    }

    @Test
    @Order(7)
    void reviewFindingCreatesAuditEvent() {
        findingId = given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/findings")
        .then()
                .statusCode(200)
                .extract().path("[0].id");

        given()
                .header("X-User", "architect")
                .header("X-Roles", "ARCHITECT")
                .contentType(ContentType.JSON)
                .body(Map.of("status", "APPROVED", "reviewNote", "Validado"))
        .when()
                .patch("/api/v1/assessments/" + assessmentId + "/findings/" + findingId)
        .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"));

        given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/audit")
        .then()
                .statusCode(200)
                .body("find { it.action == 'FINDING_STATUS_CHANGED' }", notNullValue());
    }

    @Test
    @Order(8)
    void publishedSnapshotIsImmutable() {
        snapshotId = given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/reports/generate")
        .then()
                .statusCode(200)
                .extract().path("id");

        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/reports/" + snapshotId + "/publish")
        .then()
                .statusCode(200)
                .body("published", equalTo(true));

        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/reports/" + snapshotId + "/publish")
        .then()
                .statusCode(400);
    }
}
