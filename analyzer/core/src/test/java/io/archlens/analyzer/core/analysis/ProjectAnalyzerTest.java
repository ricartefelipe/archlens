package io.archlens.analyzer.core.analysis;

import io.archlens.analyzer.core.json.AnalysisJson;
import io.archlens.analyzer.core.model.AnalysisDocument;
import io.archlens.analyzer.core.schema.SchemaValidator;
import io.archlens.analyzer.core.testsupport.WorkspacePaths;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectAnalyzerTest {

    private static Path sampleRoot() {
        return WorkspacePaths.sampleLegacyDemo();
    }

    private static Path schemaPath() {
        return WorkspacePaths.analysisSchema();
    }

    @Test
    void scanSampleProducesValidJson() throws Exception {
        AnalysisDocument document = ProjectAnalyzer.analyze(sampleRoot());
        String json = AnalysisJson.writePretty(document);

        SchemaValidator.ValidationResult result = SchemaValidator.validateJson(schemaPath(), json);
        assertTrue(result.success(), result.message());

        assertEquals("1.0.0", document.schemaVersion());
        assertNotNull(document.contentHash());
        assertTrue(document.contentHash().startsWith("sha256:"));
        assertFalse(document.signals().isEmpty());
        assertFalse(document.dependencies().isEmpty());
        assertTrue(document.metrics().javaFileCount() >= 8);
    }

    @Test
    void scanDetectsLegacySignals() throws Exception {
        AnalysisDocument document = ProjectAnalyzer.analyze(sampleRoot());
        var signalNames = document.signals().stream().map(s -> s.id()).toList();

        assertTrue(signalNames.contains("build.maven"));
        assertTrue(signalNames.contains("ee.javax-namespace") || signalNames.contains("ee.ejb"));
        assertTrue(signalNames.contains("api.jax-rs"));
        assertTrue(signalNames.contains("persistence.jpa"));
        assertTrue(signalNames.contains("quality.insufficient-tests"));
    }

    @Test
    void contentHashIsDeterministic() throws Exception {
        AnalysisDocument first = ProjectAnalyzer.analyze(sampleRoot());
        AnalysisDocument second = ProjectAnalyzer.analyze(sampleRoot());

        String firstHash = AnalysisJson.computeContentHash(first);
        String secondHash = AnalysisJson.computeContentHash(second);

        assertEquals(firstHash, secondHash);
        assertEquals(first.contentHash(), second.contentHash());
    }

    @Test
    void doesNotLeakFictionalSecretOrFullSource() throws Exception {
        AnalysisDocument document = ProjectAnalyzer.analyze(sampleRoot());
        String json = AnalysisJson.writePretty(document);
        assertFalse(json.contains("demo-not-a-real-secret-123"));
        assertFalse(json.contains("package com.demo"));
        assertFalse(json.contains("private static final String password"));
        assertNotNull(document.durationMs());
        assertTrue(document.durationMs() >= 0);
        assertFalse(document.secrets().isEmpty());
        assertFalse(document.evidences().isEmpty());
        assertEquals("8", document.project().javaVersionHint());
    }

    @Test
    void doesNotExecutePayloadAndOmitsShellSecret(@TempDir Path tempDir) throws Exception {
        Path marker = tempDir.resolve("EXECUTED");
        Files.writeString(tempDir.resolve("pom.xml"), """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>demo</groupId>
                  <artifactId>demo</artifactId>
                  <version>1</version>
                </project>
                """);
        Files.createDirectories(tempDir.resolve("src/main/java"));
        Files.writeString(tempDir.resolve("src/main/java/App.java"), """
                class App {
                  void danger() { Runtime.getRuntime().exec("touch EXECUTED"); }
                }
                """);
        Files.writeString(tempDir.resolve("run.sh"), """
                #!/bin/sh
                touch EXECUTED
                password="fixture-shell-secret-not-real"
                """);
        Files.createDirectories(tempDir.resolve("secret-dir"));
        Files.writeString(tempDir.resolve("secret-dir/Hidden.java"), "class Hidden {}");

        AnalysisDocument document = ProjectAnalyzer.analyze(
                tempDir,
                io.archlens.analyzer.core.walk.WalkLimits.DEFAULT.withExclusions(java.util.Set.of("secret-dir"))
        );
        String json = AnalysisJson.writePretty(document);
        assertFalse(Files.exists(marker));
        assertFalse(json.contains("fixture-shell-secret-not-real"));
        assertFalse(json.contains("class Hidden"));
        assertFalse(json.contains("Runtime.getRuntime"));
        assertTrue(document.secrets().stream().anyMatch(secret -> "PASSWORD".equals(secret.type())));
        assertTrue(document.signals().stream().anyMatch(signal -> signal.id().startsWith("walk.diagnostic")));
        SchemaValidator.ValidationResult result = SchemaValidator.validateJson(schemaPath(), json);
        assertTrue(result.success(), result.message());
    }
}
