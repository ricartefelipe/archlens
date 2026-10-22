package io.archlens.application.service;

import io.archlens.application.model.AnalysisDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentHashVerifierTest {

    @Test
    void demoFixtureHashMatchesCanonicalPayload() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        AnalysisDocument doc = new ObjectMapper().readValue(json, AnalysisDocument.class);
        assertTrue(ContentHashVerifier.isSupportedSchema(doc.schemaVersion()));
        assertTrue(ContentHashVerifier.matches(doc), ContentHashVerifier.compute(doc));
    }

    @Test
    void changedFieldBreaksHashUntilRecomputed() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        AnalysisDocument doc = new ObjectMapper().readValue(json, AnalysisDocument.class);
        AnalysisDocument mutated = new AnalysisDocument(
                doc.schemaVersion(),
                doc.analyzerVersion(),
                doc.generatedAt(),
                doc.durationMs(),
                doc.contentHash(),
                new AnalysisDocument.Project("outro", doc.project().buildSystems(), doc.project().languages(), doc.project().javaVersionHint()),
                doc.inventory(),
                doc.signals(),
                doc.dependencies(),
                doc.hotspots(),
                doc.cycles(),
                doc.secrets(),
                doc.evidences(),
                doc.metrics()
        );
        assertFalse(ContentHashVerifier.matches(mutated));
        AnalysisDocument restamped = new ObjectMapper().readValue(
                ContentHashVerifier.writeWithHash(mutated), AnalysisDocument.class);
        assertTrue(ContentHashVerifier.matches(restamped));
        assertEquals("outro", restamped.project().rootName());
    }
}
