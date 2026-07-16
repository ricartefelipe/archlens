package io.archlens.application.service;

import io.archlens.application.model.AnalysisDocument;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
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
}
