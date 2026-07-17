package io.archlens;

import io.archlens.infrastructure.persistence.OrganizationEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrgIsolationIT {

    static UUID orgA;
    static UUID orgB;
    static String assessmentA;
    static String findingA;
    static String snapshotA;
    static final String DEMO_JSON;

    static {
        try {
            DEMO_JSON = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    static void ensureOrgs() {
        orgA = upsertOrg("Org Isolation A");
        orgB = upsertOrg("Org Isolation B");
        assertNotEquals(orgA, orgB);
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

    @Test
    @Order(1)
    void createAssessmentsInTwoOrgs() {
        assessmentA = given()
                .header("X-User", "a")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", orgA.toString())
                .contentType(ContentType.JSON)
                .body(Map.of("name", "A-Assessment"))
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .extract().path("id");

        String assessmentB = given()
                .header("X-User", "b")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", orgB.toString())
                .contentType(ContentType.JSON)
                .body(Map.of("name", "B-Assessment"))
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .extract().path("id");

        assertNotNull(assessmentA);
        assertNotNull(assessmentB);
        assertNotEquals(assessmentA, assessmentB);
    }

    @Test
    @Order(2)
    void crossOrgAssessmentIs404() {
        given()
                .header("X-User", "b")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", orgB.toString())
        .when()
                .get("/api/v1/assessments/" + assessmentA)
        .then()
                .statusCode(404)
                .contentType(containsString("application/problem+json"));
    }

    @Test
    @Order(3)
    void importAndFindingStayIsolated() {
        given()
                .header("X-User", "a")
                .header("X-Roles", "ARCHITECT")
                .header("X-Organization-Id", orgA.toString())
                .header("Idempotency-Key", "org-a-import")
                .contentType(ContentType.JSON)
                .body(DEMO_JSON)
        .when()
                .post("/api/v1/assessments/" + assessmentA + "/import")
        .then()
                .statusCode(200)
                .body("findingsCount", greaterThan(0));

        findingA = given()
                .header("X-User", "a")
                .header("X-Roles", "VIEWER")
                .header("X-Organization-Id", orgA.toString())
        .when()
                .get("/api/v1/assessments/" + assessmentA + "/findings")
        .then()
                .statusCode(200)
                .extract().path("[0].id");

        given()
                .header("X-User", "b")
                .header("X-Roles", "VIEWER")
                .header("X-Organization-Id", orgB.toString())
        .when()
                .get("/api/v1/assessments/" + assessmentA + "/findings")
        .then()
                .statusCode(404);

        given()
                .header("X-User", "b")
                .header("X-Roles", "ARCHITECT")
                .header("X-Organization-Id", orgB.toString())
                .contentType(ContentType.JSON)
                .body(Map.of("status", "APPROVED", "reviewNote", "leak?"))
        .when()
                .patch("/api/v1/assessments/" + assessmentA + "/findings/" + findingA)
        .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    void reportSnapshotCrossOrgIs404() {
        snapshotA = given()
                .header("X-User", "a")
                .header("X-Roles", "ARCHITECT")
                .header("X-Organization-Id", orgA.toString())
        .when()
                .post("/api/v1/assessments/" + assessmentA + "/reports/generate")
        .then()
                .statusCode(200)
                .extract().path("id");

        given()
                .header("X-User", "b")
                .header("X-Roles", "VIEWER")
                .header("X-Organization-Id", orgB.toString())
        .when()
                .get("/api/v1/assessments/" + assessmentA + "/reports/" + snapshotA + "/html")
        .then()
                .statusCode(404);

        given()
                .header("X-User", "b")
                .header("X-Roles", "ARCHITECT")
                .header("X-Organization-Id", orgB.toString())
        .when()
                .post("/api/v1/assessments/" + assessmentA + "/reports/" + snapshotA + "/publish")
        .then()
                .statusCode(404);

        given()
                .header("X-User", "a")
                .header("X-Roles", "VIEWER")
                .header("X-Organization-Id", orgA.toString())
        .when()
                .get("/api/v1/assessments/" + assessmentA + "/reports/" + snapshotA + "/html")
        .then()
                .statusCode(200)
                .body(containsString("Relatório ArchLens"));
    }
}
