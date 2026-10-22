package io.archlens.application.service;

import io.archlens.api.exception.BusinessException;
import io.archlens.infrastructure.adapter.MinimizedNarrativePrompt;
import io.archlens.infrastructure.adapter.OpenAiCompatibleNarrativeGenerator;
import io.archlens.infrastructure.security.HeaderIdentityPolicy;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClosurePolicyTest {

    @Test
    void productionRejectsForgedHeadersAndDemoDoesNot() {
        assertFalse(HeaderIdentityPolicy.headerIdentityAllowed(true, "production"));
        assertFalse(HeaderIdentityPolicy.organizationHeaderTrusted("production"));
        assertTrue(HeaderIdentityPolicy.headerIdentityAllowed(true, "demo"));
        assertTrue(HeaderIdentityPolicy.headerIdentityAllowed(true, "local"));
        assertTrue(HeaderIdentityPolicy.headerIdentityAllowed(true, "test"));
        assertFalse(HeaderIdentityPolicy.headerIdentityAllowed(false, "local"));
    }

    @Test
    void legalHoldBlocksDeletion() {
        assertThrows(BusinessException.class, () -> DeletionPolicy.ensureDeletionAllowed(true));
        DeletionPolicy.ensureDeletionAllowed(false);
    }

    @Test
    void blobCleanupFailureStaysPendingAndRetryCompletes() {
        BlobCleanupPlanner.Task failed = BlobCleanupPlanner.afterAttempt(
                new BlobCleanupPlanner.Task(0, "PENDING"),
                true,
                () -> {
                    throw new IllegalStateException("disco cheio");
                }
        );
        assertEquals("PENDING", failed.status());
        assertEquals(1, failed.attempts());

        BlobCleanupPlanner.Task done = BlobCleanupPlanner.afterAttempt(failed, true, () -> {
        });
        assertEquals("DONE", done.status());
        assertEquals(1, done.attempts());
    }

    @Test
    void canonicalHashMatchesFixtureAndRejectsNothingExtra() throws Exception {
        String raw = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        String declared = raw.split("\"contentHash\" : \"")[1].split("\"")[0];
        assertEquals(declared, CanonicalContentHash.compute(raw));
    }

    @Test
    void canonicalHashIgnoresVolatileFieldsAndKeyOrder() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode tree = mapper.readTree(Files.readString(Path.of("src/test/resources/demo-analysis.json")));
        String original = CanonicalContentHash.compute(mapper.writeValueAsString(tree));

        ObjectNode reordered = mapper.createObjectNode();
        reordered.set("signals", tree.get("signals"));
        reordered.put("generatedAt", "1999-01-01T00:00:00Z");
        reordered.put("durationMs", 1);
        tree.fieldNames().forEachRemaining(name -> {
            if (!reordered.has(name)) {
                reordered.set(name, tree.get(name));
            }
        });
        assertEquals(original, CanonicalContentHash.compute(mapper.writeValueAsString(reordered)));

        ((ObjectNode) tree).put("analyzerVersion", "outra-versao");
        assertFalse(original.equals(CanonicalContentHash.compute(mapper.writeValueAsString(tree))));
    }

    @Test
    void externalNarrativePromptOmitsPathsAndHashes() {
        String prompt = MinimizedNarrativePrompt.build(3, "BUSINESS_CRITICALITY", "62.50");
        assertFalse(prompt.contains(".java"));
        assertFalse(prompt.contains("sha256:"));
        assertFalse(prompt.contains("/tmp"));
        assertTrue(prompt.contains("não") || prompt.contains("Não"));
        assertEquals("texto", OpenAiCompatibleNarrativeGenerator.extractContent("{\"content\":\"texto\"}"));
    }
}
