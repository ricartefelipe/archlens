package io.archlens.application.service;

import io.archlens.PostgresTestResource;
import io.archlens.application.model.AnalysisDocument;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@QuarkusTestResource(PostgresTestResource.class)
class RuleEngineTest {

    @Inject
    RuleLoader ruleLoader;
    @Inject
    RuleEngine ruleEngine;
    @Inject
    com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Test
    void loadsAtLeast25Rules() {
        assertTrue(ruleLoader.count() >= 25, "Expected >= 25 rules, got " + ruleLoader.count());
    }

    @Test
    void evaluatesRulesAgainstFixture() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/valid-analysis.json"));
        AnalysisDocument doc = objectMapper.readValue(json, AnalysisDocument.class);
        var findings = ruleEngine.evaluate(UUID.randomUUID(), UUID.randomUUID(), doc);
        assertFalse(findings.isEmpty());
        assertTrue(findings.stream().allMatch(f -> f.evidenceIds != null && !f.evidenceIds.isBlank()));
    }

    @Test
    void evaluatesRealAnalyzerDemoJson() throws Exception {
        String json = Files.readString(Path.of("src/test/resources/demo-analysis.json"));
        AnalysisDocument doc = objectMapper.readValue(json, AnalysisDocument.class);
        var findings = ruleEngine.evaluate(UUID.randomUUID(), UUID.randomUUID(), doc);
        assertFalse(findings.isEmpty(), "Fixture real do analyzer deve gerar findings");
        assertTrue(findings.stream().allMatch(f -> f.evidenceIds != null && !f.evidenceIds.isBlank()));
        System.out.println("DEMO_FINDINGS_COUNT=" + findings.size());
        assertTrue(findings.size() >= 5, "Esperado >= 5 findings na fixture demo, got " + findings.size());
    }

    @Test
    void emptySignalsIsDeterministic() {
        AnalysisDocument empty = new AnalysisDocument(
                "1.0.0",
                "0.1.0-SNAPSHOT",
                "2026-07-17T12:00:00Z",
                1L,
                "sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                new AnalysisDocument.Project("empty", List.of("MAVEN"), List.of("JAVA"), "21"),
                new AnalysisDocument.Inventory(List.of(), List.of(), List.of(), List.of(), List.of(), List.of()),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new AnalysisDocument.Metrics(1, 0, 1, 0, 0, 0)
        );
        var first = ruleEngine.evaluate(UUID.randomUUID(), UUID.randomUUID(), empty);
        var second = ruleEngine.evaluate(UUID.randomUUID(), UUID.randomUUID(), empty);
        assertEquals(first.size(), second.size());
        assertTrue(first.stream().allMatch(f -> f.evidenceIds != null && !f.evidenceIds.isBlank())
                || first.isEmpty());
    }
}
