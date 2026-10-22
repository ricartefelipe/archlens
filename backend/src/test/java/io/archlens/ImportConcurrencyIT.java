package io.archlens;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.archlens.application.service.CanonicalContentHash;
import io.archlens.infrastructure.persistence.AnalysisImportEntity;
import io.archlens.infrastructure.persistence.ImportIdempotencyAliasEntity;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class ImportConcurrencyIT {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void concurrentImportsOfTheSameArtifactCollapseToOneRow() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        String assessmentId = createAssessment("Concorrência hash");
        int threads = 8;
        List<String> keys = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            keys.add("race-" + i);
        }

        ConcurrentLinkedQueue<Integer> statuses = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> importIds = new ConcurrentLinkedQueue<>();
        postTogether(assessmentId, keys, json, statuses, importIds);

        assertEquals(threads, statuses.size());
        assertTrue(statuses.stream().allMatch(code -> code == 200), statuses.toString());
        assertEquals(1, Set.copyOf(importIds).size(), importIds.toString());

        UUID assessment = UUID.fromString(assessmentId);
        long rows = QuarkusTransaction.requiringNew().call(() ->
                AnalysisImportEntity.count("assessmentId", assessment));
        long aliases = QuarkusTransaction.requiringNew().call(() ->
                ImportIdempotencyAliasEntity.count("assessmentId", assessment));
        assertEquals(1, rows);
        assertEquals(threads - 1, aliases);

        String winner = importIds.peek();
        for (String key : keys) {
            String replay = importJson(assessmentId, key, json);
            assertEquals(winner, replay);
        }

        String other = withAnalyzerVersion(json, "0.1.0-concurrent");
        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", keys.get(1))
                .contentType(ContentType.JSON)
                .body(other)
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(409)
                .body("detail", containsString("Idempotency-Key"));

        long rowsAfterConflict = QuarkusTransaction.requiringNew().call(() ->
                AnalysisImportEntity.count("assessmentId", assessment));
        assertEquals(1, rowsAfterConflict);
    }

    @Test
    void concurrentReuseOfOneIdempotencyKeyDoesNotDuplicate() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        String assessmentId = createAssessment("Concorrência chave");
        List<String> keys = List.of("same", "same", "same", "same", "same", "same");
        ConcurrentLinkedQueue<Integer> statuses = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> importIds = new ConcurrentLinkedQueue<>();
        postTogether(assessmentId, keys, json, statuses, importIds);

        assertTrue(statuses.stream().allMatch(code -> code == 200), statuses.toString());
        assertEquals(1, Set.copyOf(importIds).size(), importIds.toString());
        long rows = QuarkusTransaction.requiringNew().call(() ->
                AnalysisImportEntity.count("assessmentId", UUID.fromString(assessmentId)));
        assertEquals(1, rows);
    }

    @Test
    void invalidSchemaIsRejectedBeforePersistence() throws Exception {
        String assessmentId = createAssessment("Schema inválido");
        given()
                .header("X-User", "tester")
                .header("X-Roles", "ARCHITECT")
                .header("Idempotency-Key", "bad-schema")
                .contentType(ContentType.JSON)
                .body("{\"schemaVersion\":\"0\"}")
        .when()
                .post("/api/v1/assessments/" + assessmentId + "/import")
        .then()
                .statusCode(400);

        long rows = QuarkusTransaction.requiringNew().call(() ->
                AnalysisImportEntity.count("assessmentId", UUID.fromString(assessmentId)));
        assertEquals(0, rows);
    }

    private static void postTogether(
            String assessmentId,
            List<String> keys,
            String json,
            ConcurrentLinkedQueue<Integer> statuses,
            ConcurrentLinkedQueue<String> importIds) throws Exception {
        int threads = keys.size();
        CyclicBarrier barrier = new CyclicBarrier(threads);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (String key : keys) {
                futures.add(pool.submit(() -> {
                    barrier.await(20, TimeUnit.SECONDS);
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create("http://127.0.0.1:" + RestAssured.port
                                    + "/api/v1/assessments/" + assessmentId + "/import"))
                            .timeout(Duration.ofSeconds(30))
                            .header("X-User", "tester")
                            .header("X-Roles", "ARCHITECT")
                            .header("Idempotency-Key", key)
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(json))
                            .build();
                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    statuses.add(response.statusCode());
                    if (response.statusCode() == 200) {
                        JsonNode body = MAPPER.readTree(response.body());
                        importIds.add(body.get("importId").asText());
                    }
                    return null;
                }));
            }
            for (Future<?> future : futures) {
                future.get(40, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    private static String withAnalyzerVersion(String raw, String version) throws Exception {
        ObjectNode node = (ObjectNode) MAPPER.readTree(raw);
        node.put("analyzerVersion", version);
        node.put("contentHash", CanonicalContentHash.compute(MAPPER.writeValueAsString(node)));
        return MAPPER.writeValueAsString(node);
    }

    private static String createAssessment(String name) {
        return given()
                .header("X-User", "tester")
                .header("X-Roles", "ADMIN")
                .contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"description\":\"concorrência\"}")
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
}
