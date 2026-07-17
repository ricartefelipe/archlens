package io.archlens;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ArchLensApiIT {

    static String assessmentId;
    static String importId;
    static String findingId;
    static String snapshotId;
    static String optionId;
    static String publishedHtmlHash;
    static int findingsCount;

    static final String DEMO_JSON;
    static final String VALID_JSON;

    static {
        try {
            DEMO_JSON = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
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
                .header("X-Correlation-Id", not(emptyOrNullString()))
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
                .body("status", equalTo("DRAFT"))
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
                .contentType(containsString("application/problem+json"))
                .body("correlationId", not(emptyOrNullString()));
    }

    @Test
    @Order(4)
    void oversizedPayloadRejected() {
        String big = "{\"schemaVersion\":\"1.0.0\",\"pad\":\"" + "x".repeat(6_000_000) + "\"}";
        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "too-big")
                .contentType(ContentType.JSON)
                .body(big)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(anyOf(is(413), is(400)));
    }

    @Test
    @Order(5)
    void realAnalyzerImportMaterializesAndGeneratesFindings() {
        findingsCount = given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "import-demo-1")
                .contentType(ContentType.JSON)
                .body(DEMO_JSON)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(200)
                .body("findingsCount", greaterThan(0))
                .body("contentHash", startsWith("sha256:"))
                .extract().path("findingsCount");

        importId = given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/findings")
        .then()
                .statusCode(200)
                .body("size()", equalTo(findingsCount))
                .body("[0].evidenceIds", not(empty()))
                .extract().path("[0].id");

        findingId = importId;

        given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/inventory")
        .then()
                .statusCode(200)
                .body("modules.size()", greaterThan(0))
                .body("types.size()", greaterThan(0));
    }

    @Test
    @Order(6)
    void importIsIdempotentAndConflictsOnDifferentPayload() {
        String first = given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "idem-key-stable")
                .contentType(ContentType.JSON)
                .body(DEMO_JSON)
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
                .body(DEMO_JSON)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(200)
                .extract().path("importId");

        assertEquals(first, second);

        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "idem-key-stable")
                .contentType(ContentType.JSON)
                .body(VALID_JSON)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(409)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @Order(7)
    void questionnaireChangesScoresAndRejectsOutOfRange() {
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
                .body(Map.of("answers", Map.of("business_criticality", 9)))
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/questionnaire")
        .then()
                .statusCode(400);

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
                .body("find { it.dimensionKey == 'BUSINESS_CRITICALITY' }.rationale",
                        containsString("apoiam decisão"))
                .extract().jsonPath().getDouble("find { it.dimensionKey == 'BUSINESS_CRITICALITY' }.score");

        assertTrue(after > before, "Questionário deve aumentar BUSINESS_CRITICALITY");
    }

    @Test
    @Order(8)
    void reviewFindingCreatesAuditEvent() {
        given()
                .header("X-User", "architect")
                .header("X-Roles", "ARCHITECT")
                .contentType(ContentType.JSON)
                .body(Map.of("status", "APPROVED", "reviewNote", "Validado"))
        .when()
                .patch("/api/v1/assessments/" + assessmentId + "/findings/" + findingId)
        .then()
                .statusCode(200)
                .body("status", equalTo("APPROVED"))
                .body("reviewedBy", equalTo("architect"));

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
    @Order(9)
    void modernizationAndRoadmap() {
        optionId = given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Strangler Fig",
                        "description", "Extrair APIs",
                        "effort", "HIGH",
                        "impact", "HIGH"
                ))
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/modernization-options")
        .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "Fase 1 — estabilizar",
                        "description", "Hotfixes e observabilidade",
                        "phase", "SHORT_TERM",
                        "sortOrder", 1,
                        "modernizationOptionId", optionId
                ))
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/roadmap")
        .then()
                .statusCode(201)
                .body("phase", equalTo("SHORT_TERM"));

        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "title", "bad",
                        "description", "x",
                        "phase", "MAGIC",
                        "sortOrder", 1
                ))
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/roadmap")
        .then()
                .statusCode(400);
    }

    @Test
    @Order(10)
    void reportHtmlPdfAndImmutableSnapshot() {
        snapshotId = given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/reports/generate")
        .then()
                .statusCode(200)
                .body("contentHash", startsWith("sha256:"))
                .extract().path("id");

        String html = given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/reports/" + snapshotId + "/html")
        .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .header("Content-Disposition", containsString("report-v"))
                .extract().asString();

        assertTrue(html.contains("Relatório ArchLens"));
        assertTrue(html.contains("Limitações"));
        assertTrue(html.contains("Roadmap"));
        publishedHtmlHash = given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/reports")
        .then()
                .statusCode(200)
                .extract().path("find { it.id == '" + snapshotId + "' }.contentHash");

        byte[] pdf = given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/reports/" + snapshotId + "/pdf")
        .then()
                .statusCode(200)
                .contentType(containsString("application/pdf"))
                .header("Content-Disposition", containsString(".pdf"))
                .extract().asByteArray();

        assertTrue(pdf.length >= 100);
        assertEquals("%PDF", new String(pdf, 0, 4, StandardCharsets.US_ASCII));

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
                .statusCode(409);

        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Demo Legacy Renamed"))
        .when()
                .put("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(200);

        String htmlAfter = given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/reports/" + snapshotId + "/html")
        .then()
                .statusCode(200)
                .extract().asString();

        assertTrue(htmlAfter.contains("Demo Legacy"));
        assertFalse(htmlAfter.contains("Demo Legacy Renamed"));
        assertEquals(publishedHtmlHash, given()
                .header("X-User", "tester")
                .header("X-Roles", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/reports")
        .then()
                .extract().path("find { it.id == '" + snapshotId + "' }.contentHash"));
    }

    @Test
    @Order(11)
    void completedBlocksImport() {
        given()
                .header("X-User", "tester")
                .header("X-Roles", "ADMIN")
                .contentType(ContentType.JSON)
                .body(Map.of("status", "COMPLETED"))
        .when()
                .put("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));

        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "after-complete")
                .contentType(ContentType.JSON)
                .body(DEMO_JSON)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(400);
    }
}
