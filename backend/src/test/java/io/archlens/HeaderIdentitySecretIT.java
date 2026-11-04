package io.archlens;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.UnaryOperator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Perfil com headers locais ligados: a identidade só vale com o segredo do servidor do frontend.
 * Sem ele (ou com ele errado, ou sem {@code X-User}), toda rota de dados responde 401; com ele,
 * o papel decide (VIEWER não escreve) e outra organização responde 404.
 */
@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class HeaderIdentitySecretIT {

    record Route(String method, String path, String body) {
    }

    static String assessmentId;
    static String findingId;

    @BeforeEach
    void createOwnedData() throws Exception {
        if (assessmentId != null) {
            return;
        }
        assessmentId = asUser("dono", "ADMIN")
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Avaliação do dono"))
        .when()
                .post("/api/v1/assessments")
        .then()
                .statusCode(201)
                .extract().path("id");

        asUser("dono", "ARCHITECT")
                .header("Idempotency-Key", "segredo-it-" + UUID.randomUUID())
                .contentType(ContentType.JSON)
                .body(Files.readString(Path.of("src/test/resources/demo-analysis.json")))
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(200);

        findingId = asUser("dono", "VIEWER")
        .when()
                .get("/api/v1/assessments/" + assessmentId + "/findings")
        .then()
                .statusCode(200)
                .extract().path("[0].id");
    }

    private static RequestSpecification asUser(String user, String roles) {
        return given()
                .header(TestIdentity.SECRET_HEADER, TestIdentity.SECRET)
                .header("X-User", user)
                .header("X-Roles", roles);
    }

    /** Todas as rotas de dados da API, com corpo válido o bastante para chegar à checagem de identidade. */
    private static List<Route> dataRoutes() {
        String a = "/api/v1/assessments/" + assessmentId;
        String other = UUID.randomUUID().toString();
        return List.of(
                new Route("GET", "/api/v1/assessments", null),
                new Route("POST", "/api/v1/assessments", "{\"name\":\"intrusa\"}"),
                new Route("GET", a, null),
                new Route("PUT", a, "{\"name\":\"renomeada\"}"),
                new Route("DELETE", a, null),
                new Route("POST", a + "/import", "{}"),
                new Route("GET", a + "/findings", null),
                new Route("GET", a + "/findings/" + findingId, null),
                new Route("PATCH", a + "/findings/" + findingId, "{\"status\":\"ACCEPTED\"}"),
                new Route("GET", a + "/findings/export.csv", null),
                new Route("GET", a + "/inventory", null),
                new Route("GET", a + "/questionnaire", null),
                new Route("POST", a + "/questionnaire", "{\"answers\":[]}"),
                new Route("GET", a + "/dimensions", null),
                new Route("GET", a + "/modernization-options", null),
                new Route("POST", a + "/modernization-options", "{\"title\":\"x\"}"),
                new Route("PUT", a + "/modernization-options/" + other, "{\"title\":\"x\"}"),
                new Route("DELETE", a + "/modernization-options/" + other, null),
                new Route("GET", a + "/roadmap", null),
                new Route("POST", a + "/roadmap", "{\"title\":\"x\"}"),
                new Route("PUT", a + "/roadmap/" + other, "{\"title\":\"x\"}"),
                new Route("DELETE", a + "/roadmap/" + other, null),
                new Route("POST", a + "/reports/generate", "{}"),
                new Route("GET", a + "/reports", null),
                new Route("POST", a + "/reports/" + other + "/publish", "{}"),
                new Route("GET", a + "/reports/" + other + "/html", null),
                new Route("GET", a + "/reports/" + other + "/pdf", null),
                new Route("GET", "/api/v1/audit", null),
                new Route("GET", "/api/v1/organization/summary", null),
                new Route("POST", "/api/v1/maintenance/blob-cleanup", null));
    }

    private static int call(Route route, UnaryOperator<RequestSpecification> auth) {
        RequestSpecification spec = auth.apply(given()).header("Idempotency-Key", "k-" + UUID.randomUUID());
        if (route.body() != null) {
            spec = spec.contentType(ContentType.JSON).body(route.body());
        }
        return spec.when().request(route.method(), route.path()).then().extract().statusCode();
    }

    private static void expectEverywhere(int expected, UnaryOperator<RequestSpecification> auth) {
        List<String> wrong = new ArrayList<>();
        for (Route route : dataRoutes()) {
            int status = call(route, auth);
            if (status != expected) {
                wrong.add(route.method() + " " + route.path() + " -> " + status);
            }
        }
        assertTrue(wrong.isEmpty(), "esperado " + expected + " em todas as rotas, mas: " + wrong);
    }

    @Test
    void withoutAnyCredentialEveryDataRouteIs401() {
        expectEverywhere(401, spec -> spec);
    }

    @Test
    void forgedIdentityWithoutSecretIs401() {
        expectEverywhere(401, spec -> spec
                .header("X-User", "intruso")
                .header("X-Roles", "ADMIN"));
    }

    @Test
    void wrongSecretIs401() {
        expectEverywhere(401, spec -> spec
                .header(TestIdentity.SECRET_HEADER, TestIdentity.SECRET + "-errado")
                .header("X-User", "intruso")
                .header("X-Roles", "ADMIN"));
        expectEverywhere(401, spec -> spec
                .header(TestIdentity.SECRET_HEADER, "")
                .header("X-User", "intruso")
                .header("X-Roles", "ADMIN"));
    }

    @Test
    void secretWithoutUserIs401() {
        expectEverywhere(401, spec -> spec
                .header(TestIdentity.SECRET_HEADER, TestIdentity.SECRET)
                .header("X-Roles", "ADMIN"));
    }

    @Test
    void foreignOrganizationIs404AndIsIgnoredWithoutSecret() {
        String foreignOrg = UUID.randomUUID().toString();
        asUser("dono", "ADMIN")
                .header("X-Organization-Id", foreignOrg)
        .when()
                .get("/api/v1/assessments/" + assessmentId)
        .then()
                .statusCode(404);

        given()
                .header("X-User", "intruso")
                .header("X-Roles", "ADMIN")
                .header("X-Organization-Id", foreignOrg)
        .when()
                .get("/api/v1/assessments")
        .then()
                .statusCode(401);
    }

    @Test
    void ownerReadsAndViewerCannotWrite() {
        for (String path : List.of(
                "/api/v1/assessments",
                "/api/v1/assessments/" + assessmentId,
                "/api/v1/assessments/" + assessmentId + "/findings",
                "/api/v1/assessments/" + assessmentId + "/findings/export.csv",
                "/api/v1/assessments/" + assessmentId + "/inventory",
                "/api/v1/assessments/" + assessmentId + "/reports",
                "/api/v1/audit",
                "/api/v1/organization/summary")) {
            assertEquals(200, asUser("dono", "VIEWER").when().get(path).then().extract().statusCode(), path);
        }

        assertEquals(403, asUser("visitante", "VIEWER")
                .contentType(ContentType.JSON)
                .body(Map.of("name", "não deveria existir"))
                .when().post("/api/v1/assessments").then().extract().statusCode());
        assertEquals(403, asUser("visitante", "VIEWER")
                .when().delete("/api/v1/assessments/" + assessmentId).then().extract().statusCode());
        assertEquals(403, asUser("arquiteto", "ARCHITECT")
                .when().post("/api/v1/maintenance/blob-cleanup").then().extract().statusCode());
        assertEquals(201, asUser("arquiteto", "ARCHITECT")
                .contentType(ContentType.JSON)
                .body(Map.of("name", "Criada pelo arquiteto"))
                .when().post("/api/v1/assessments").then().extract().statusCode());
    }

    @Test
    void missingRolesHeaderMeansViewerNotAdmin() {
        assertEquals(403, given()
                .header(TestIdentity.SECRET_HEADER, TestIdentity.SECRET)
                .header("X-User", "sem-papel")
                .contentType(ContentType.JSON)
                .body(Map.of("name", "sem papel"))
                .when().post("/api/v1/assessments").then().extract().statusCode());
    }

    @Test
    void publicHealthStaysOpen() {
        given().when().get("/api/v1/health").then().statusCode(200);
        given().when().get("/q/health/ready").then().statusCode(200);
    }
}
