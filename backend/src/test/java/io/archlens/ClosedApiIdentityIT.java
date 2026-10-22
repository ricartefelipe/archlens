package io.archlens;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestProfile(ClosedIdentityProfile.class)
@QuarkusTestResource(PostgresTestResource.class)
class ClosedApiIdentityIT {

    @Test
    void productionStyleProfileRejectsMissingAndForgedIdentity() {
        given()
        .when()
                .get("/api/v1/assessments")
        .then()
                .statusCode(401);

        given()
                .header("X-User", "intruso")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", "11111111-1111-4111-8111-111111111111")
        .when()
                .get("/api/v1/assessments")
        .then()
                .statusCode(401);

        given()
                .header("X-User", "intruso")
                .header("X-Roles", "ADMIN")
        .when()
                .delete("/api/v1/assessments/00000000-0000-4000-8000-000000000099")
        .then()
                .statusCode(401);

        given()
                .header("X-User", "intruso")
                .header("X-Roles", "ADMIN")
        .when()
                .post("/api/v1/maintenance/blob-cleanup")
        .then()
                .statusCode(401);

        given()
        .when()
                .get("/api/v1/health")
        .then()
                .statusCode(200);
    }
}
