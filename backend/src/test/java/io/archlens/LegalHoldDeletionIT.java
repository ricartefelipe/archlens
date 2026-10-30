package io.archlens;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

@QuarkusTest
@TestProfile(LegalHoldProfile.class)
@QuarkusTestResource(PostgresTestResource.class)
class LegalHoldDeletionIT {

    @Test
    void legalHoldRejectsDeleteAndKeepsTheAssessment() {
        String assessmentId = given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
                .contentType(ContentType.JSON)
                .body("{\"name\":\"Retenção\",\"description\":\"hold\"}")
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .delete("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(400)
                .body("detail", containsString("Retenção legal"));

        given()
                .header("X-User", "admin")
                .header("X-Roles", "ADMIN")
        .when()
                .get("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(200);
    }
}
